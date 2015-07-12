package gc.adjudicaciones

import org.openqa.selenium.firefox.FirefoxDriver

import scala.concurrent.ExecutionContext.Implicits.global

object AdjudicacionesRecolector extends App with DbConfig {
  import slick.driver.SQLiteDriver
  val driver = SQLiteDriver

  AdjudicacionesCrawler.asIterator(new FirefoxDriver()).duplicate match {
    case (it1, it2) => insertOrUpdate(it1).zip(it2) foreach { case (recordsUpdatedF, adjudicacion) =>
      recordsUpdatedF foreach { recordsUpdated =>
        logger.info(s"Upserted [$recordsUpdated] $adjudicacion")
      }
    }
  }
}
