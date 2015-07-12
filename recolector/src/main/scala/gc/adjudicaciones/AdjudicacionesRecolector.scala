package gc.adjudicaciones

import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object AdjudicacionesRecolector extends App {
  val logger = LoggerFactory.getLogger(AdjudicacionesRecolector.getClass)

  AdjudicacionesCrawler.asIterator(new FirefoxDriver()).duplicate match {
    case (it1, it2) => AdjudicacionesTable.insertOrUpdate(it1).zip(it2) foreach { case (recordsUpdatedF, adjudicacion) =>
      recordsUpdatedF foreach { recordsUpdated =>
        logger.info(s"Upserted [$recordsUpdated] $adjudicacion")
      }
    }
  }
}
