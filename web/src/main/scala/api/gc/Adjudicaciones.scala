package api.gc

import java.time.{LocalDate, Year, YearMonth}

import akka.http.scaladsl.server.Directives._
import db.DbConfig
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import gc.adjudicaciones.{Adjudicacion, Proveedor}
import play.api.libs.json.Json

object Adjudicaciones extends DbConfig {
  import slick.driver.PostgresDriver
  val driver = PostgresDriver
  import driver.api._

  implicit val proveedorFmt = Json.format[Proveedor]
  implicit val adjudicacionFmt = Json.format[Adjudicacion]

  val route =
    pathPrefix("adjudicaciones") {
      path(IntNumber) { ano =>
        val year = Year.of(ano)
        val query = adjudicaciones.filter(adj => adj.fecha >= year.atDay(1) && adj.fecha <= year.atMonth(12).atEndOfMonth)
        onSuccess(db.run(query.result))(complete(_))
      } ~
      path(IntNumber / IntNumber) { (ano, mes) =>
        val yearMonth = YearMonth.of(ano, mes)
        val query = adjudicaciones.filter(adj => adj.fecha >= yearMonth.atDay(1) && adj.fecha <= yearMonth.atEndOfMonth)
        onSuccess(db.run(query.result))(complete(_))
      } ~
      path(IntNumber / IntNumber / IntNumber) { (ano, mes, dia) =>
        val date = LocalDate.of(ano, mes, dia)
        val query = adjudicaciones.filter(_.fecha === date)
        onSuccess(db.run(query.result))(complete(_))
      }
    }
}
