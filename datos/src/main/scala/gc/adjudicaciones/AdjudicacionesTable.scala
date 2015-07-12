package gc.adjudicaciones

import java.time.LocalDate

import org.slf4j.LoggerFactory
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AdjudicacionTable {
  protected val driver: JdbcProfile
  import driver.api._

  class Adjudicaciones(tag: Tag) extends Table[Adjudicacion](tag, "ADJUDICACIONES") {
    def nog = column[Long]("NOG", O.PrimaryKey)
    def fecha = column[LocalDate]("FECHA")
    def idProveedor = column[Option[Long]]("ID_PROVEEDOR")
    def nombreProveedor = column[String]("NOMBRE_PROVEEDOR")
    def nit = column[Option[String]]("NIT")
    def pais = column[Option[String]]("PAIS")
    def monto = column[BigDecimal]("MONTO")

    def proveedor = (idProveedor, nombreProveedor) <> (Proveedor.tupled, Proveedor.unapply)
    def * = (nog, fecha, proveedor, nit, pais, monto) <> (Adjudicacion.tupled, Adjudicacion.unapply)

    def idx_fecha = index("IDX_ADJUDICACIONES_FECHA", fecha)
  }

  val adjudicaciones = TableQuery[Adjudicaciones]

  implicit lazy val fechaColumnType = MappedColumnType.base[LocalDate, String](
    date => date.toString,
    str => LocalDate.parse(str)
  )
}

trait DbConfig extends AdjudicacionTable {
  val logger = LoggerFactory.getLogger(this.getClass)

  protected val driver: JdbcProfile
  import driver.api._

  lazy val db = Database.forURL("jdbc:sqlite:datafiles/adjudicaciones.sqlite", driver="org.sqlite.JDBC")

  def setup = {
    db.run(adjudicaciones.schema.create) andThen {
      case result => logger.info(result.toString)
    }
  }

  def insertOrUpdate(records: Iterator[Adjudicacion]): Iterator[Future[Int]] = {
    for (record <- records) yield db.run(adjudicaciones.insertOrUpdate(record))
  }
}
