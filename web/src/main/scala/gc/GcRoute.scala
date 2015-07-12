package gc

import akka.http.scaladsl.server.Directives._

object GcRoute {
  val route =
    pathPrefix("gc") {
      gc.adjudicaciones.AdjudicacionesRoute.route
    }
}
