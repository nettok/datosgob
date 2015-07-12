package gc.adjudicaciones

import org.openqa.selenium.firefox.FirefoxDriver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import db.DbConfig

object AdjudicacionesRecolector extends App with DbConfig {
  import slick.driver.SQLiteDriver
  val driver = SQLiteDriver
  import driver.api._

  setupDb

  AdjudicacionesCrawler.asIterator(new FirefoxDriver()).duplicate match {
    case (it1, it2) => insertOrUpdate(it1).zip(it2) foreach { case (recordsUpdatedF, adjudicacion) =>
      recordsUpdatedF foreach { recordsUpdated =>
        logger.info(s"Upserted [$recordsUpdated] $adjudicacion")
      }
    }
  }

  def insertOrUpdate(records: Iterator[Adjudicacion]): Iterator[Future[Int]] = {
    for (record <- records) yield db.run(adjudicaciones.insertOrUpdate(record))
  }
}
