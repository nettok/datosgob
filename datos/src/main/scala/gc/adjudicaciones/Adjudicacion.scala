package gc.adjudicaciones

import java.time.LocalDate

case class Proveedor(id: Option[Long], nombre: String)

case class Adjudicacion(nog: Long, fecha: LocalDate, proveedor: Proveedor,
                        nit: Option[String], pais: Option[String], monto: BigDecimal)
