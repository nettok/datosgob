// Robot: "Iñigo"

import java.net.URI
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.openqa.selenium.{Proxy, WebDriver, WebElement, By}
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.duration._


case class TablaResultado(tabla: WebElement, filaTitulo: WebElement, filaPagineo: WebElement, filasDatos: Seq[WebElement]) {
  val columnaFecha = filaTitulo.findElement(By.tagName("td"))
  val paginaActual = filaPagineo.findElements(By.tagName("span")).get(1)

  val paginaSiguiente: Option[WebElement] = filaPagineo.findElements(By.xpath("./td/*")).asScala.dropWhile(we =>
    we.getText != paginaActual.getText
  ).slice(1, 2).headOption
}

case class Proveedor(id: Option[Long], nombre: String)

case class Adjudicacion(fecha: LocalDate, proveedor: Proveedor, nitOPais: Either[String, String], monto: BigDecimal, nog: Long)

object U {
  def waitForTablaResultado(browser: WebDriver, timeout: FiniteDuration): TablaResultado = {
    val tabla = new WebDriverWait(browser, timeout.toSeconds).until(
      ExpectedConditions.presenceOfElementLocated(By.id("MasterGC_ContentBlockHolder_dgResultado")))

    val filaTitulo = tabla.findElement(By.className("TablaTitulo"))
    val filaPagineo = tabla.findElement(By.className("TablaPagineo"))

    val filasDatos = tabla.findElements(By.tagName("tr")).asScala.filter(we =>
      List("TablaFila1", "TablaFila2").contains(we.getAttribute("class")))

    TablaResultado(tabla, filaTitulo, filaPagineo, filasDatos)
  }

  def waitForTablaResultadoUpdate(browser: WebDriver, timeout: FiniteDuration, oldTablaResultado: TablaResultado): TablaResultado = {
    new WebDriverWait(browser, timeout.toSeconds).until(
      ExpectedConditions.stalenessOf(oldTablaResultado.tabla))

    waitForTablaResultado(browser, 1.second)
  }

  def readFilaDatos(filaDatos: WebElement): Adjudicacion = {
    // Obtener texto de las columnas

    val cols = filaDatos.findElements(By.tagName("td"))
    val fechaTexto = cols.get(0).getText
    val nombreProveedor = cols.get(1).getText
    val nitOPaisTexto = cols.get(2).getText
    val montoTexto = cols.get(3).getText
    val nogTexto = cols.get(4).getText

    // Extraer el identificador del proveedor del URI

    val proveedorHref = cols.get(1).findElement(By.tagName("a")).getAttribute("href")

    val idProveedor: Option[Long] =
      if (proveedorHref != null) {
        new URI(proveedorHref).getQuery.split('&').map { param =>
          val keyValue = param.split('=')
          (keyValue(0), keyValue(1))
        }.toMap.get("lprv").map(_.toLong)
      } else {
        None
      }

    // Ensamblar resultado

    val fecha = readFecha(fechaTexto)
    val proveedor = Proveedor(idProveedor, nombreProveedor)
    val nitOPais = if (idProveedor.isDefined) Left(nitOPaisTexto) else Right(nitOPaisTexto)
    val monto = readMonto(montoTexto)
    val nog = nogTexto.toLong

    Adjudicacion(fecha, proveedor, nitOPais, monto, nog)
  }

  def readFecha(fechaTexto: String): LocalDate = {
    val splitted = fechaTexto.split('.')

    splitted(1) =
      splitted(1) match {
        case "ene" => "01"
        case "feb" => "02"
        case "mar" => "03"
        case "abr" => "04"
        case "may" => "05"
        case "jun" => "06"
        case "jul" => "07"
        case "ago" => "08"
        case "sep" => "09"
        case "oct" => "10"
        case "nov" => "11"
        case "dic" => "12"
      }

    val fechaTextoFixed = splitted.mkString(".")

    LocalDate.parse(fechaTextoFixed, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
  }

  private val decimalFormat: DecimalFormat = {
    val pattern = "#,#00.0#"
    val groupingSeparator = ','
    val decimalSeparator = '.'

    val symbols = new DecimalFormatSymbols()
    symbols.setGroupingSeparator(groupingSeparator)
    symbols.setDecimalSeparator(decimalSeparator)

    val decimalFormat = new DecimalFormat(pattern, symbols)
    decimalFormat.setParseBigDecimal(true)

    decimalFormat
  }

  def readMonto(montoTexto: String): BigDecimal = {
    BigDecimal(decimalFormat.parse(montoTexto).asInstanceOf[java.math.BigDecimal])
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

  var tabla = U.waitForTablaResultado(browser, waitTimeout)
  assert(tabla.paginaActual.getText == "1", "Tenemos que estar en la primera pagina")
  assert(tabla.columnaFecha.getText == "Fecha de adjudicación▼",
    """El resultado debe estar ordenado por "Fecha de adjudicación" en orden descendente""")

  // Ordenar el resultado en orden ascendente
  // y esperar a que se actualice el resultado (ajax)

  var cambiarOrden = tabla.columnaFecha.findElement(By.tagName("a"))
  cambiarOrden.click()

  tabla = U.waitForTablaResultadoUpdate(browser, waitTimeout, tabla)
  assert(tabla.columnaFecha.getText == "Fecha de adjudicación▲", "El resultado debe estar en orden ascendente")

  // Obtener registros de la pagina actual
  // Cambiar a la siguiente pagina si existe y repetir

  @tailrec
  def obtenerDatos(tabla: TablaResultado): Unit = {
    for (fila <- tabla.filasDatos) {
      println(U.readFilaDatos(fila))
    }

    tabla.paginaSiguiente match {
      case Some(pagSig) =>
        pagSig.click()
        obtenerDatos(U.waitForTablaResultadoUpdate(browser, waitTimeout, tabla))
      case None =>
    }
  }

  obtenerDatos(tabla)
}