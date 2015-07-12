package gc.adjudicaciones

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import play.api.libs.json.Json

object AdjudicacionesRoute {
  import AdjudicacionesTable._
  import slick.driver.SQLiteDriver.api._

  implicit val proveedorFmt = Json.format[Proveedor]
  implicit val adjudicacionFmt = Json.format[Adjudicacion]

  val route =
    path("adjudicaciones") {
      onSuccess(db.run(adjudicaciones.result)) { result =>
        complete(result)
      }
    }
}
