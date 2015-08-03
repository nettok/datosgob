package gc.adjudicaciones

import java.time.LocalDate

import org.openqa.selenium.firefox.FirefoxDriver
//import org.openqa.selenium.phantomjs.PhantomJSDriver

import scala.collection.immutable.ListSet
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import db.DbConfig

import gc.adjudicaciones.scraper.AdjudicacionesScraper

object AdjudicacionesRecolectorOfDateRange extends App with DbConfig {
  import slick.driver.PostgresDriver
  val driver = PostgresDriver
  import driver.api._

  setupDb

  val webDriver = new FirefoxDriver()

  /* Es esta la primera vez que ejecutamos este recolector?  Donde nos quedamos la ultima vez?
   * Debemos continuar desde donde nos quedamos.
   */

  // primero registro disponible en GuateCompras
  val firstLastScrapedF = Future { AdjudicacionesScraper.firstLast(webDriver) }

  // ultimo registro obtenido de GuateCompras y almacenado en la base de datos
  val latestStoredF = db.run(adjudicaciones.sortBy(_.fecha.desc).take(1).result.headOption)

  // ejecutar en paralelo
  val currentState = firstLastScrapedF.zip(latestStoredF)

  // obtener rango de fechas a recolectar
  val startEnd = Await.result(
    currentState map {
      case (firstLatest, latestStored) =>
        latestStored match {
          case Some(adj) => (adj.fecha, firstLatest._2.fecha)
          case None => (firstLatest._1.fecha, firstLatest._2.fecha)
        }
    }, 30.seconds
  )

  /* Empezar a recolectar las adjudicaciones de cada rango mensual.
   *
   * Separar la recoleccion por mes facilita la recuperacion de fallas
   * de forma eficiente y permite paralelizar la recoleccion.
   */

  // dividir el rango de fechas completo por meses
  val pendingMonths: Seq[(LocalDate, LocalDate)] = {
    val start = startEnd._1
    val end = startEnd._2

    val years = Stream.range(start.getYear, end.getYear + 1)
    val months = (Stream(start.getMonthValue to 12) ++ Stream.continually(1 to 12)).toIterator
    val firstDays = (Stream(start.getDayOfMonth) ++ Stream.continually(1)).toIterator

    val froms = for {
      year <- years
      month <- months.next()
      from = LocalDate.of(year, month, firstDays.next())
      if from.isBefore(end) || from.isEqual(end)
    } yield from

    froms.map { from =>
      val to = from.withDayOfMonth(from.lengthOfMonth)
      (from, to)
    }
  }

  // extraer y almacenar los datos mes a mes
  val scrapeMonths = pendingMonths map { case (from, to) =>
    val scrapeDateRangeF = Future.sequence(scrapeDateRange(from, to))

    scrapeDateRangeF onSuccess {
      case _ => logger.info(s"Scraped date range from $from to $to")
    }

    scrapeDateRangeF onFailure {
      case e: Throwable => logger.error(s"Scrape date range failed from $from to $to\n\t-- $e")
    }

    scrapeDateRangeF
  }

  // TODO: reintentar meses fallidos
  scrapeMonths.foreach { scrapeMonth =>
    Await.ready(scrapeMonth, Duration.Inf)
  }

  def scrapeDateRange(from: LocalDate, to: LocalDate) = {
    AdjudicacionesScraper.asIteratorOfDateRange(webDriver, from, to).grouped(50).map { batch =>
      val (from, to) = batchFromTo(batch)

      val insertBatch = db.run(adjudicaciones ++= batch.distinct)

      insertBatch onSuccess {
        case Some(recordsInserted) =>
          logger.info(s"Inserted $recordsInserted of ${batch.length} records from $from to $to")
      }

      insertBatch onFailure {
        case e: java.sql.BatchUpdateException =>
          logger.error(s"Insert failed from $from to $to\n\t-- $e\n\t-- ${e.getNextException}")
      }

      insertBatch recoverWith  {
        case e: java.sql.BatchUpdateException =>
          val insertBatchRetry = retryBatchInsertFailure(batch)

          insertBatchRetry onSuccess {
            case (retrySet, Some(recordsInserted)) =>
              logger.info(s"(Retry) Inserted $recordsInserted of ${retrySet.size} records from $from to $to")
          }

          insertBatchRetry onFailure {
            case e: java.sql.BatchUpdateException =>
              logger.error(s"(Retry) Insert failed from $from to $to\n\t-- $e\n\t-- ${e.getNextException}")
          }

          insertBatchRetry
      }
    }.toList
  }

  def batchFromTo(batch: Seq[Adjudicacion]) = {
    val first = batch.head
    val last = batch.last
    val from = (first.fecha, first.nog)
    val to = (last.fecha, last.nog)
    (from, to)
  }

  /**
   * Reintenta insertar las adjudicaciones del `batch` excluyendo las adjudicaciones conflictivas
   */
  def retryBatchInsertFailure(batch: Seq[Adjudicacion]) = {
    val (from, to) = batchFromTo(batch)
    val (fromDate, toDate) = (from._1, to._1)
    require(fromDate.compareTo(toDate) <= 0, "fecha ascendente")

    val batchSet = ListSet(batch: _*)

    val excludeQuery = adjudicaciones
      .filter(adj => adj.fecha >= fromDate && adj.fecha <= toDate && adj.nog.inSet(batch.map(_.nog)))

    db.run(excludeQuery.result) map { exclude =>
      batchSet.diff(ListSet(exclude: _*))
    } flatMap { retrySet =>
      db.run(adjudicaciones ++= retrySet) map { recordsInserted =>
        (retrySet, recordsInserted)
      }
    }
  }
}
