// Robot: "Iñigo"

import java.net.URI

import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.openqa.selenium.{Proxy, WebDriver, WebElement, By}
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

import scala.collection.JavaConverters._
import scala.concurrent.duration._


case class TablaResultado(tabla: WebElement, filaTitulo: WebElement, filaPagineo: WebElement, filasDatos: Seq[WebElement]) {
  val columnaFecha = filaTitulo.findElement(By.tagName("td"))
  val paginaActual = filaPagineo.findElements(By.tagName("span")).get(1)
}


object B {
  def waitForResultTable(browser: WebDriver, timeout: FiniteDuration): TablaResultado = {
    val tabla = new WebDriverWait(browser, timeout.toSeconds).until(
      ExpectedConditions.presenceOfElementLocated(By.id("MasterGC_ContentBlockHolder_dgResultado")))

    val filaTitulo = tabla.findElement(By.className("TablaTitulo"))
    val filaPagineo = tabla.findElement(By.className("TablaPagineo"))

    val filasDatos = tabla.findElements(By.tagName("tr")).asScala.filter(we =>
      List("TablaFila1", "TablaFila2").contains(we.getAttribute("class")))

    TablaResultado(tabla, filaTitulo, filaPagineo, filasDatos)
  }

  def waitForResultTableUpdate(browser: WebDriver, timeout: FiniteDuration, oldTablaResultado: TablaResultado): TablaResultado = {
    new WebDriverWait(browser, timeout.toSeconds).until(
      ExpectedConditions.stalenessOf(oldTablaResultado.tabla))

    waitForResultTable(browser, 1.second)
  }
}


object ConcursosAdjudicados extends App {
  val waitTimeout = 10.seconds
  val waitTimeoutSeconds = waitTimeout.toSeconds

  // Iniciar instancia del browser

  val proxy = new Proxy().setSocksProxy("localhost:8888")
  val capabilities = new DesiredCapabilities()
  capabilities.setCapability(CapabilityType.PROXY, proxy)

  val browser = new FirefoxDriver(capabilities)

  // Navegar a la pagina de consultas de adjudicaciones

  browser.get("http://www.guatecompras.gt/proveedores/consultaadvprovee.aspx")

  // Encontrar "Opción 1: Buscar TODAS las adjudicaciones"

  val opcion1Id = "MasterGC_ContentBlockHolder_rdbOpciones_0"
  val opcion1Input = browser.findElement(By.id(opcion1Id))
  val opcion1Label = browser.findElement(By.cssSelector(s"""label[for="$opcion1Id"]"""))
  assert(opcion1Label.getText == "Opción 1: Buscar TODAS las adjudicaciones", "Esta debe ser la opcion 1")

  // Realizar consulta haciendo clic en esta opcion
  // y esperar a que se actualice la pagina con el resultado (ajax)

  opcion1Input.click()

  var tabla = B.waitForResultTable(browser, waitTimeout)
  assert(tabla.paginaActual.getText == "1", "Tenemos que estar en la primera pagina")
  assert(tabla.columnaFecha.getText == "Fecha de adjudicación▼",
    """El resultado debe estar ordenado por "Fecha de adjudicación" en orden descendente""")

  // Ordenar el resultado en orden ascendente
  // y esperar a que se actualice el resultado (ajax)

  var cambiarOrden = tabla.columnaFecha.findElement(By.tagName("a"))
  cambiarOrden.click()

  tabla = B.waitForResultTableUpdate(browser, waitTimeout, tabla)
  assert(tabla.columnaFecha.getText == "Fecha de adjudicación▲", "El resultado debe estar en orden ascendente")

  // Obtener registros de la pagina actual

  // TODO
  for (fila <- tabla.filasDatos) {
    // Obtener texto de las columnas

    val cols = fila.findElements(By.tagName("td"))
    val fechaAdjudicacion = cols.get(0).getText
    val proveedor = cols.get(1).getText
    val nitOPais = cols.get(2).getText
    val monto = cols.get(3).getText
    val nog = cols.get(4).getText

    // Obtener identificador del proveedor

    val proveedorHref = cols.get(1).findElement(By.tagName("a")).getAttribute("href")

    val idProveedor: Option[String] =
      if (proveedorHref != null) {
        new URI(proveedorHref).getQuery.split('&').map { param =>
          val keyValue = param.split('=')
          (keyValue(0), keyValue(1))
        }.toMap.get("lprv")
      } else {
        None
      }

    println(fechaAdjudicacion, proveedor, nitOPais, monto, nog, idProveedor)
  }

  // TODO: Pasar a la siguiente pagina si existe

  // TODO: Ir a: Obtener registro de la pagina actual
}
