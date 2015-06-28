import org.openqa.selenium.firefox.FirefoxDriver

object Recolector extends App {
  val browser = new FirefoxDriver()

  ConcursosAdjudicadosCrawler.asIterator(browser).foreach { adjudicacion =>
    println(adjudicacion)
  }
}
