import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object Recolector extends App {
  val logger = LoggerFactory.getLogger(Recolector.getClass)

  ConcursosAdjudicadosCrawler.asIterator(new FirefoxDriver()).duplicate match {
    case (it1, it2) => DB.insert(it1).zip(it2) foreach { case (recordsUpdatedF, adjudicacion) =>
      recordsUpdatedF foreach { recordsUpdated =>
        logger.info(s"Inserted [$recordsUpdated] $adjudicacion")
      }
    }
  }
}
