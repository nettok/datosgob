package gc.adjudicaciones

import org.openqa.selenium.phantomjs.PhantomJSDriver

import scala.collection.immutable.ListSet
import scala.concurrent.ExecutionContext.Implicits.global

import db.DbConfig

object AdjudicacionesRecolectorFromFirstToLast extends App with DbConfig {
  import slick.driver.PostgresDriver
  val driver = PostgresDriver
  import driver.api._

  setupDb

  AdjudicacionesCrawler.asIteratorFromFirstToLast(new PhantomJSDriver()).grouped(50).foreach { batch =>
    val (from, to) = batchFromTo(batch)

    val insertBatch = db.run(adjudicaciones ++= batch.distinct)

    insertBatch onSuccess {
      case Some(recordsInserted) =>
        logger.info(s"Inserted $recordsInserted of ${batch.length} records from $from to $to")
    }

    insertBatch onFailure {
      case e: java.sql.BatchUpdateException =>
        logger.error(s"Insert failed from $from to $to\n\t-- $e\n\t-- ${e.getNextException}")
        val insertBatchRetry = retryBatchInsertFailure(batch)

        insertBatchRetry onSuccess {
          case (retrySet, Some(recordsInserted)) =>
            logger.info(s"(Retry) Inserted $recordsInserted of ${retrySet.size} records from $from to $to")
        }

        insertBatchRetry onFailure {
          case e: java.sql.BatchUpdateException =>
            logger.error(s"(Retry) Insert failed from $from to $to\n\t-- $e\n\t-- ${e.getNextException}")
        }
    }
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
