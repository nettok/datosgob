package gc.adjudicaciones

import java.time.LocalDate

case class Proveedor(nombre: String, id: Option[Long], nit: Option[String], pais: Option[String])

case class Adjudicacion(fecha: LocalDate, nog: Long, proveedor: Proveedor, monto: BigDecimal)
