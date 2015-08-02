package gc.adjudicaciones.scraper

import java.net.URI
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import org.openqa.selenium._
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

import gc.adjudicaciones.{Adjudicacion, Proveedor}

case class TablaResultado(tabla: WebElement, filaTitulo: WebElement, filaPagineo: WebElement, filasDatos: Seq[WebElement]) {
  val columnaFecha = filaTitulo.findElement(By.tagName("td"))
  val paginaActual = filaPagineo.findElements(By.tagName("span")).get(1)

  val enlacesPaginas = filaPagineo.findElements(By.xpath("./td/*")).asScala.dropWhile(_.getText != paginaActual.getText)
  val paginaSiguiente: Option[WebElement] = enlacesPaginas.slice(1, 2).headOption
}

class AdjudicacionesScraper private (val browser: RemoteWebDriver, val timeout: FiniteDuration) {

  private val timeoutSeconds = timeout.toSeconds

  private def start() = {
    // Navegar a la pagina de consultas de adjudicaciones

    browser.get("http://www.guatecompras.gt/proveedores/consultaadvprovee.aspx")
  }

  private def opcion1(): TablaResultado = {
    // Encontrar "Opción 1: Buscar TODAS las adjudicaciones"

    val opcion1Id = "MasterGC_ContentBlockHolder_rdbOpciones_0"
    val opcion1Input = browser.findElement(By.id(opcion1Id))
    val opcion1Label = browser.findElement(By.cssSelector(s"""label[for="$opcion1Id"]"""))
    assert(opcion1Label.getText == "Opción 1: Buscar TODAS las adjudicaciones", "Esta debe ser la opcion 1")

    // Realizar consulta haciendo clic en esta opcion
    // y esperar a que se actualice la pagina con el resultado (ajax)

    opcion1Input.click()

    val tabla = waitForTablaResultado
    assert(tabla.paginaActual.getText == "1", "Tenemos que estar en la primera pagina")
    assert(tabla.columnaFecha.getText == "Fecha de adjudicación▼",
      """El resultado debe estar ordenado por "Fecha de adjudicación" en orden descendente""")

    tabla
  }

  private def opcion5(from: LocalDate, to: LocalDate): TablaResultado = {
    // Encontrar "Opción 5: Buscar por fecha de adjudicación, monto adjudicado o tipo de proveedor"

    val opcion5Id = "MasterGC_ContentBlockHolder_rdbOpciones_4"
    val opcion5Input = browser.findElement(By.id(opcion5Id))
    val opcion5Label = browser.findElement(By.cssSelector(s"""label[for="$opcion5Id"]"""))
    assert(opcion5Label.getText == "Opción 5: Buscar por fecha de adjudicación, monto adjudicado o tipo de proveedor",
      "Esta debe ser la opcion 5")

    // Seleccionar esta opcion
    // y esperar el formulario de parametros de busqueda y establecer el rango de tiempo (ajax)

    opcion5Input.click()

    val form = new WebDriverWait(browser, timeoutSeconds).until(
      ExpectedConditions.presenceOfElementLocated(By.id("MasterGC_ContentBlockHolder_Table2")))

    val jsExec = browser.asInstanceOf[JavascriptExecutor]
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MMMM.yyyy", new Locale("es"))

    jsExec.executeScript(s"$$('#MasterGC_ContentBlockHolder_txtFechaIni').val('${from.format(dateFormatter)}')")
    jsExec.executeScript(s"$$('#MasterGC_ContentBlockHolder_txtFechaFin').val('${to.format(dateFormatter)}')")

    val consultarButton = form.findElement(By.id("MasterGC_ContentBlockHolder_Button1"))

    // Hacer la consulta

    consultarButton.click()

    val tabla = waitForTablaResultado
    assert(tabla.paginaActual.getText == "1", "Tenemos que estar en la primera pagina")
    assert(tabla.columnaFecha.getText == "Fecha de adjudicación▼",
      """El resultado debe estar ordenado por "Fecha de adjudicación" en orden descendente""")

    tabla
  }

  private def orderByFechaAsc(tabla: TablaResultado): TablaResultado = {
    // Ordenar el resultado en orden ascendente
    // y esperar a que se actualice el resultado (ajax)

    val cambiarOrden = tabla.columnaFecha.findElement(By.tagName("a"))
    cambiarOrden.click()

    val newTabla = waitForTablaResultadoUpdate(tabla)
    assert(newTabla.columnaFecha.getText == "Fecha de adjudicación▲", "El resultado debe estar en orden ascendente")

    newTabla
  }

  private def irAPagina(tablaActual: TablaResultado, enlacePagina: WebElement): TablaResultado = {
    enlacePagina.click()
    waitForTablaResultadoUpdate(tablaActual)
  }

  private def irAPaginaSiguiente(tablaActual: TablaResultado): Option[TablaResultado] = {
    tablaActual.paginaSiguiente.map(p => irAPagina(tablaActual, p))
  }

  private def iterator(tabla: TablaResultado): Iterator[Adjudicacion] = {
    // Obtener registros de la pagina actual
    // Cambiar a la siguiente pagina si existe y repetir

    val paginas = new Iterator[TablaResultado] {
      var primeraVez = true
      var tablaActual: Option[TablaResultado] = Some(tabla)
      def hasNext = primeraVez || tablaActual.get.paginaSiguiente.isDefined
      def next(): TablaResultado = {
        if (!primeraVez) {
          tablaActual  = irAPaginaSiguiente(tablaActual.get)
        } else {
          primeraVez = false
        }

        tablaActual.get
      }
    }

    val adjudicaciones =
      for {
        pagina <- paginas
        fila <- pagina.filasDatos
      } yield readFilaDatos(fila)

    adjudicaciones
  }

  private def waitForTablaResultado: TablaResultado = {
    val tabla = new WebDriverWait(browser, timeoutSeconds).until(
      ExpectedConditions.presenceOfElementLocated(By.id("MasterGC_ContentBlockHolder_dgResultado")))

    val filaTitulo = tabla.findElement(By.className("encabezado1"))
    val filaPagineo = tabla.findElement(By.className("TablaPagineo"))

    val filasDatos = tabla.findElements(By.tagName("tr")).asScala.filter(we =>
      List("TablaFilaMix1", "TablaFilaMix2").contains(we.getAttribute("class")))

    TablaResultado(tabla, filaTitulo, filaPagineo, filasDatos)
  }

  private def waitForTablaResultadoUpdate(oldTablaResultado: TablaResultado): TablaResultado = {
    new WebDriverWait(browser, timeoutSeconds).until(
      ExpectedConditions.stalenessOf(oldTablaResultado.tabla))

    waitForTablaResultado
  }

  private def readFilaDatos(filaDatos: WebElement): Adjudicacion = {
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
    val (nit, pais) = if (idProveedor.isDefined) (Some(nitOPaisTexto), None) else (None, Some(nitOPaisTexto))
    val proveedor = Proveedor(nombreProveedor, idProveedor, nit, pais)
    val monto = readMonto(montoTexto)
    val nog = nogTexto.toLong

    Adjudicacion(fecha, nog, proveedor, monto)
  }

  private def readFecha(fechaTexto: String): LocalDate = {
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

  private def readMonto(montoTexto: String): BigDecimal = {
    BigDecimal(decimalFormat.parse(montoTexto).asInstanceOf[java.math.BigDecimal])
  }
}

object AdjudicacionesScraper {
  val defaultTimeout = 30.seconds

  private def startScraper(browser: RemoteWebDriver, timeout: FiniteDuration = defaultTimeout): AdjudicacionesScraper = {
    val scraper = new AdjudicacionesScraper(browser, timeout)
    scraper.start()
    scraper
  }

  def asIteratorFromFirstToLast(browser: RemoteWebDriver, timeout: FiniteDuration = defaultTimeout): Iterator[Adjudicacion]  = {
    val scraper = startScraper(browser, timeout)
    scraper.iterator(scraper.orderByFechaAsc(scraper.opcion1()))
  }

  def asIteratorFromLastToFirst(browser: RemoteWebDriver, timeout: FiniteDuration = defaultTimeout): Iterator[Adjudicacion]  = {
    val scraper = startScraper(browser, timeout)
    scraper.iterator(scraper.opcion1())
  }

  def asIteratorOfDateRange(browser: RemoteWebDriver, from: LocalDate, to: LocalDate, timeout: FiniteDuration = defaultTimeout): Iterator[Adjudicacion]  = {
    val scraper = startScraper(browser, timeout)
    scraper.iterator(scraper.opcion5(from, to))
  }

  def firstLast(browser: RemoteWebDriver, timeout: FiniteDuration = defaultTimeout): (Adjudicacion, Adjudicacion) = {
    val scraper = startScraper(browser, timeout)
    val tabla = scraper.opcion1()
    val last = scraper.readFilaDatos(tabla.filasDatos.head)
    val first = scraper.readFilaDatos(scraper.orderByFechaAsc(tabla).filasDatos.head)
    (first, last)
  }
}
