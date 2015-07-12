package gc.adjudicaciones

import java.time.LocalDate
import slick.driver.JdbcProfile

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
