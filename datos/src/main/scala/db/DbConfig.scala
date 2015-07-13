package db

import gc.adjudicaciones.AdjudicacionTable
import org.slf4j.LoggerFactory
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

trait DbConfig extends AdjudicacionTable {
  val logger = LoggerFactory.getLogger(this.getClass)

  protected val driver: JdbcProfile
  import driver.api._

  lazy val db = Database.forConfig("postgres")

  def setupDb = {
    db.run(adjudicaciones.schema.create) andThen {
      case result => logger.info(result.toString)
    }
  }
}