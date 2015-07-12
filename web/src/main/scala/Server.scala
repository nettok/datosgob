import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn

object Server extends App {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  val route = gc.GcRoute.route

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  println(s"Server online at http://0.0.0.0:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  import system.dispatcher // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.terminate()) // and shutdown when done
}
