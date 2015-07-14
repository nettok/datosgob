package gc.adjudicaciones

import java.time.LocalDate
import slick.driver.JdbcProfile

trait AdjudicacionTable {
  protected val driver: JdbcProfile
  import driver.api._

  class Adjudicaciones(tag: Tag) extends Table[Adjudicacion](tag, "ADJUDICACIONES") {
    def fecha = column[LocalDate]("FECHA")
    def nog = column[Long]("NOG")
    def nombreProveedor = column[String]("NOMBRE_PROVEEDOR")
    def idProveedor = column[Option[Long]]("ID_PROVEEDOR")
    def nit = column[Option[String]]("NIT")
    def pais = column[Option[String]]("PAIS")
    def monto = column[BigDecimal]("MONTO")

    def proveedor = (nombreProveedor, idProveedor, nit, pais) <> (Proveedor.tupled, Proveedor.unapply)
    def * = (fecha, nog, proveedor, monto) <> (Adjudicacion.tupled, Adjudicacion.unapply)

    def pk = primaryKey("PK_ADJUDICACIONES", (nog, nombreProveedor))
    def idx_fecha = index("IDX_ADJUDICACIONES_FECHA", fecha)
  }

  val adjudicaciones = TableQuery[Adjudicaciones]

  implicit lazy val fechaColumnType = MappedColumnType.base[LocalDate, String](
    date => date.toString,
    str => LocalDate.parse(str)
  )
}
