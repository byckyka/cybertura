package io.github.byckyka.cybertura.research

import java.net.HttpURLConnection
import java.net.URL

/**
 * Envía las pestañas del informe a una hoja de Google Sheets del investigador, a través de un
 * Google Apps Script publicado como aplicación web (ver app/docs/google_sheets_script.gs).
 *
 * Solo se envía cuando el investigador pulsa el botón; si no hay internet no pasa nada, los datos
 * siguen en el teléfono. El script reemplaza las filas de cada sesión que llega, así que enviar
 * varias veces desde uno o más teléfonos no duplica nada.
 */
object GoogleSheetsUpload {

    sealed class Result {
        data class Sent(val rows: Int) : Result()
        data class Failed(val reason: String) : Result()
    }

    /** Cuerpo JSON que espera el script: la clave y las pestañas con su encabezado y filas. */
    fun payload(token: String, sheets: List<ReportSheet>): String = buildString {
        append("{\"token\":").append(json(token)).append(",\"hojas\":[")
        sheets.forEachIndexed { i, sheet ->
            if (i > 0) append(',')
            append("{\"nombre\":").append(json(sheet.name))
            append(",\"encabezado\":[").append(sheet.header.joinToString(",") { json(it) }).append(']')
            append(",\"filas\":[")
            sheet.rows.forEachIndexed { r, row ->
                if (r > 0) append(',')
                append('[').append(row.joinToString(",") { json(it) }).append(']')
            }
            append("]}")
        }
        append("]}")
    }

    /** Lee la respuesta del script: {"ok":true,"filas":N} o {"ok":false,"error":"..."}. */
    fun parseResponse(body: String): Result {
        val compact = body.replace(" ", "")
        if (compact.contains("\"ok\":true")) {
            val rows = Regex("\"filas\":(\\d+)").find(compact)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return Result.Sent(rows)
        }
        val error = Regex("\"error\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
        return Result.Failed(error ?: "La hoja no respondió como se esperaba. Revisa la dirección del script.")
    }

    /** Hace el envío. Bloquea el hilo: no llamar en el hilo principal. */
    fun send(url: String, token: String, sheets: List<ReportSheet>): Result = runCatching {
        val body = payload(token, sheets).toByteArray(Charsets.UTF_8)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 60_000
            // Apps Script responde con una redirección a la respuesta; se sigue sola
            instanceFollowRedirects = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setFixedLengthStreamingMode(body.size)
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) return Result.Failed("La hoja respondió con un error ($code).")
            parseResponse(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }.getOrElse { Result.Failed("No hay conexión a internet o la dirección del script no es válida.") }

    private fun json(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when {
                ch == '"' -> append("\\\"")
                ch == '\\' -> append("\\\\")
                ch == '\n' -> append("\\n")
                ch == '\r' -> append("\\r")
                ch == '\t' -> append("\\t")
                ch < ' ' -> append(String.format("\\u%04x", ch.code))
                else -> append(ch)
            }
        }
        append('"')
    }
}
