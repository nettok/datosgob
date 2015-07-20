package api

import akka.http.scaladsl.server.Directives._
import api.gc.Adjudicaciones

object Api {
  val route =
    pathPrefix("api") {
      pathPrefix("gc") {
        Adjudicaciones.route
      }
    }
}
