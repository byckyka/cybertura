package io.github.byckyka.cybertura

import io.github.byckyka.cybertura.research.GoogleSheetsUpload
import io.github.byckyka.cybertura.research.ReportSheet
import io.github.byckyka.cybertura.research.XlsxWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/** El Excel que se exporta y el mensaje que se envía a Google Sheets. */
class ResearchExportTest {

    private val sheets = listOf(
        ReportSheet("Resumen", listOf("Participante", "Errores", "Promedio"), listOf(listOf("P01", "3", "4.5"), listOf("01", "", "-2"))),
        ReportSheet("Guía", listOf("Cómo leer"), listOf(listOf("Texto con <símbolos> & \"comillas\" y 😄")))
    )

    /** Partes del .xlsx (es un ZIP) con su contenido. */
    private fun unzip(bytes: ByteArray): Map<String, String> {
        val parts = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { parts[it.name] = zip.readBytes().toString(Charsets.UTF_8) }
        }
        return parts
    }

    private fun xlsx(): Map<String, String> =
        unzip(ByteArrayOutputStream().also { XlsxWriter.write(sheets, it) }.toByteArray())

    @Test
    fun el_excel_trae_las_partes_que_necesita_y_una_hoja_por_pestana() {
        val parts = xlsx()
        listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
            "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml"
        ).forEach { assertTrue("Falta $it", it in parts) }
        assertTrue(parts.getValue("xl/workbook.xml").contains("""name="Resumen""""))
        assertTrue(parts.getValue("xl/workbook.xml").contains("""name="Guía""""))
    }

    @Test
    fun los_numeros_van_como_numeros_y_los_codigos_como_texto() {
        val sheet = xlsx().getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("""<c r="B2"><v>3</v></c>"""))
        assertTrue(sheet.contains("""<c r="C2"><v>4.5</v></c>"""))
        assertTrue(sheet.contains("""<c r="C3"><v>-2</v></c>"""))
        // "01" no debe perder el cero
        assertTrue(sheet.contains("""<c r="A3" t="inlineStr"><is><t xml:space="preserve">01</t></is></c>"""))
        // Una celda vacía no se escribe
        assertFalse(sheet.contains("""r="B3""""))
    }

    @Test
    fun el_texto_se_escapa_para_que_el_archivo_abra() {
        val guide = xlsx().getValue("xl/worksheets/sheet2.xml")
        assertTrue(guide.contains("Texto con &lt;símbolos&gt; &amp; &quot;comillas&quot; y 😄"))
    }

    @Test
    fun el_encabezado_queda_fijo_y_resaltado() {
        val sheet = xlsx().getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("""state="frozen""""))
        assertTrue(sheet.contains("""<c r="A1" s="1" t="inlineStr">"""))
    }

    @Test
    fun nombres_de_columna_como_en_excel() {
        assertEquals("A", XlsxWriter.columnName(0))
        assertEquals("Z", XlsxWriter.columnName(25))
        assertEquals("AA", XlsxWriter.columnName(26))
        assertEquals("AZ", XlsxWriter.columnName(51))
    }

    @Test
    fun el_mensaje_para_google_sheets_lleva_clave_pestanas_y_filas() {
        val json = GoogleSheetsUpload.payload("clave\"rara", sheets)
        assertTrue(json.startsWith("""{"token":"clave\"rara","hojas":["""))
        assertTrue(json.contains(""""nombre":"Resumen","encabezado":["Participante","Errores","Promedio"]"""))
        assertTrue(json.contains(""""filas":[["P01","3","4.5"],["01","","-2"]]"""))
        assertTrue(json.contains("""Texto con <símbolos> & \"comillas\""""))
    }

    @Test
    fun lee_la_respuesta_del_script() {
        assertEquals(GoogleSheetsUpload.Result.Sent(12), GoogleSheetsUpload.parseResponse("""{"ok":true,"filas":12}"""))
        assertEquals(
            GoogleSheetsUpload.Result.Failed("La clave no coincide con la del script."),
            GoogleSheetsUpload.parseResponse("""{"ok":false,"error":"La clave no coincide con la del script."}""")
        )
        // Una página de error de Google (HTML) no se toma como éxito
        assertTrue(GoogleSheetsUpload.parseResponse("<html>Error</html>") is GoogleSheetsUpload.Result.Failed)
    }
}
