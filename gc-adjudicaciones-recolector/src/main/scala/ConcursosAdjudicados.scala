// Robot: "Iñigo"

import org.openqa.selenium.{WebDriver, WebElement, By}
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

import scala.collection.JavaConverters._
import scala.concurrent.duration._


class TablaResultado private (tabla: WebElement, filaTitulo: WebElement, filaPagineo: WebElement) {
  def this(browser: WebDriver, timeout: FiniteDuration) {
    this(null, null, null, null)
  }
}


object ConcursosAdjudicados extends App {
  val waitTimeout = 10.seconds
  val waitTimeoutSeconds = waitTimeout.toSeconds

  val browser = new FirefoxDriver()

  // Navegar a la pagina de consultas de adjudicaciones

  browser.get("http://www.guatecompras.gt/proveedores/consultaadvprovee.aspx")

  // Encontrar "Opción 1: Buscar TODAS las adjudicaciones"

  val opcion1Id = "MasterGC_ContentBlockHolder_rdbOpciones_0"
  val opcion1Input = browser.findElement(By.id(opcion1Id))
  val opcion1Label = browser.findElement(By.cssSelector(s"""label[for="$opcion1Id"]"""))
  assert(opcion1Label.getText == "Opción 1: Buscar TODAS las adjudicaciones")

  // Realizar consulta haciendo clic en esta opcion
  // y esperar a que se actualice la pagina con el resultado (ajax)

  opcion1Input.click()

  val tablaResultadoId = "MasterGC_ContentBlockHolder_dgResultado"
  val tablaResultado = new WebDriverWait(browser, waitTimeoutSeconds).until(
    ExpectedConditions.presenceOfElementLocated(By.id(tablaResultadoId)))

  // Verificar que estamos en la pagina 1

  val filaPagineo = tablaResultado.findElement(By.className("TablaPagineo"))
  val paginaActual = filaPagineo.findElements(By.tagName("span")).get(1)
  assert(paginaActual.getText == "1")

  // Verificar que el resultado esta ordenado por "Fecha de adjudicación" en orden descendente

  val filaTitulo = tablaResultado.findElement(By.className("TablaTitulo"))
  val columnaFecha = filaTitulo.findElement(By.tagName("td"))
  assert(columnaFecha.getText == "Fecha de adjudicación▼")

  // Ordenar el resultado en orden ascendente
  // y esperar a que se actualice el resultado (ajax)

  val cambiarOrden = columnaFecha.findElement(By.tagName("a"))
  cambiarOrden.click()

  new WebDriverWait(browser, waitTimeoutSeconds).until(
    ExpectedConditions.textToBePresentInElement(columnaFecha, "Fecha de adjudicación▲"))

  // xxx

  val fila1 = tablaResultado.findElement(By.className("TablaFila1"))
  println(fila1.getText)

  def resultTableUpdated(): () = {
  }

  // yyy

  val tr = new TablaResultado(browser, waitTimeout)
}
