package gc.adjudicaciones

import org.openqa.selenium.phantomjs.PhantomJSDriver

import scala.concurrent.ExecutionContext.Implicits.global

import db.DbConfig

object AdjudicacionesRecolector extends App with DbConfig {
  import slick.driver.PostgresDriver
  val driver = PostgresDriver
  import driver.api._

  setupDb

  AdjudicacionesCrawler.asIterator(new PhantomJSDriver()).grouped(50).foreach { batch =>
    val insertBatch = db.run(adjudicaciones ++= batch)

    val first = batch.head
    val last = batch.last
    val from = (first.fecha, first.nog, first.proveedor.nombre)
    val to = (last.fecha, last.nog, last.proveedor.nombre)
    logger.info(s"Inserting batch from $from to $to")

    insertBatch.onSuccess {
      case Some(recordsInserted) => logger.info(s"Inserted $recordsInserted of ${batch.length}")
    }

    insertBatch.onFailure {
      case e: java.sql.BatchUpdateException => logger.error(s"$e\n-- ${e.getNextException}")
    }
  }
}
