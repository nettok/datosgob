import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

import scala.io.StdIn

case class Saludo(saludo: String)

object Saludo {
  implicit val fmt = Json.format[Saludo]
}

object Server extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  import PlayJsonSupport._

  val route =
    pathEndOrSingleSlash {
      get {
        complete {
          Saludo("Hola mundo!")
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  println(s"Server online at http://0.0.0.0:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  import system.dispatcher // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.terminate()) // and shutdown when done
}
