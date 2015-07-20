package ui

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._

object StaticContent {
  val route =
    pathEndOrSingleSlash {
      redirect("ui/index.html", StatusCodes.SeeOther)
    } ~
      pathPrefix("ui") {
        pathEnd {
          redirect("ui/index.html", StatusCodes.SeeOther)
        } ~
          pathSingleSlash {
            redirect("index.html", StatusCodes.SeeOther)
          } ~
          getFromDirectory("ui")
      }
}
