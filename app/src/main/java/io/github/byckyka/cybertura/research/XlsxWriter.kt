package io.github.byckyka.cybertura.research

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Escribe un libro de Excel (.xlsx) con una hoja por [ReportSheet], sin librerías externas: un
 * .xlsx es un ZIP con unos pocos XML. Cada hoja trae el encabezado resaltado y fijo al bajar,
 * columnas con ancho según su contenido, y los números como números (para sacar promedios).
 */
object XlsxWriter {

    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PKG_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships"

    // Índices de estilo de celda (cellXfs en styles.xml)
    private const val STYLE_HEADER = 1
    private const val STYLE_WRAP = 2

    /** Enteros y decimales con punto; "01" o "P01" quedan como texto. */
    private val NUMBER = Regex("""-?(0|[1-9]\d*)(\.\d+)?""")

    fun isNumber(value: String): Boolean = NUMBER.matches(value)

    fun write(sheets: List<ReportSheet>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun put(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put("_rels/.rels", rootRels())
            put("xl/workbook.xml", workbook(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, sheet -> put("xl/worksheets/sheet${i + 1}.xml", worksheet(sheet)) }
        }
    }

    // ── Partes del paquete ───────────────────────────────────────────────────

    private fun contentTypes(count: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..count) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun rootRels() =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="$PKG_REL_NS">""" +
            """<Relationship Id="rId1" Type="$REL_NS/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

    private fun workbook(sheets: List<ReportSheet>) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="$MAIN_NS" xmlns:r="$REL_NS"><sheets>""")
        sheets.forEachIndexed { i, sheet ->
            append("""<sheet name="${escape(sheetName(sheet.name))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(count: Int) = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="$PKG_REL_NS">""")
        for (i in 1..count) {
            append("""<Relationship Id="rId$i" Type="$REL_NS/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${count + 1}" Type="$REL_NS/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    /** Fuente normal; encabezado en negrita blanca sobre azul; texto que se ajusta a la celda. */
    private val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="$MAIN_NS">""" +
            """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FF1F4E79"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="3">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>""" +
            """</cellXfs>""" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            "</styleSheet>"

    private fun worksheet(sheet: ReportSheet) = buildString {
        val singleColumn = sheet.header.size == 1
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="$MAIN_NS" xmlns:r="$REL_NS">""")
        // Encabezado fijo al bajar
        append("""<sheetViews><sheetView workbookViewId="0">""")
        append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
        append("""</sheetView></sheetViews>""")
        append("""<sheetFormatPr defaultRowHeight="15"/>""")
        append("<cols>")
        sheet.header.indices.forEach { c ->
            val width = if (singleColumn) 120 else columnWidth(sheet, c)
            append("""<col min="${c + 1}" max="${c + 1}" width="$width" customWidth="1"/>""")
        }
        append("</cols><sheetData>")
        // Los encabezados largos (preguntas) se leen en varias líneas
        val headerLines = if (singleColumn) 1 else sheet.header.indices.maxOf { c ->
            (sheet.header[c].length + columnWidth(sheet, c) - 1) / columnWidth(sheet, c)
        }.coerceIn(1, 6)
        append("""<row r="1" ht="${15 * headerLines + 3}" customHeight="1">""")
        sheet.header.forEachIndexed { c, value -> append(cell(1, c, value, STYLE_HEADER)) }
        append("</row>")
        sheet.rows.forEachIndexed { r, row ->
            append("""<row r="${r + 2}">""")
            row.forEachIndexed { c, value -> append(cell(r + 2, c, value, if (singleColumn) STYLE_WRAP else 0)) }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun cell(row: Int, col: Int, value: String, style: Int): String {
        val ref = columnName(col) + row
        val s = if (style != 0) """ s="$style"""" else ""
        return when {
            value.isEmpty() -> if (style != 0) """<c r="$ref"$s/>""" else ""
            style != STYLE_HEADER && isNumber(value) -> """<c r="$ref"$s><v>$value</v></c>"""
            else -> """<c r="$ref"$s t="inlineStr"><is><t xml:space="preserve">${escape(value)}</t></is></c>"""
        }
    }

    /** Ancho según el texto más largo de la columna; los encabezados largos se parten en líneas. */
    private fun columnWidth(sheet: ReportSheet, col: Int): Int {
        val longestValue = sheet.rows.maxOfOrNull { it.getOrElse(col) { "" }.length } ?: 0
        val header = sheet.header[col].length
        return maxOf(longestValue + 2, minOf(header + 2, 24), 10).coerceAtMost(50)
    }

    /** 0 → A, 25 → Z, 26 → AA… */
    fun columnName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append('A' + rem)
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    /** Excel no admite en el nombre de una hoja \ / ? * [ ] : ni más de 31 caracteres. */
    private fun sheetName(name: String) = name.replace(Regex("""[\\/?*\[\]:]"""), " ").take(31)

    /** Escapa el texto para XML y quita caracteres de control que Excel rechaza. */
    private fun escape(text: String): String = buildString {
        for (ch in text) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r' -> append(' ')
                else -> append(ch)
            }
        }
    }
}
