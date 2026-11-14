/**
 * CyberTura: recibe los resultados del modo investigador en esta hoja de Google.
 *
 * Instalación (una sola vez):
 *  1. Crea una hoja nueva en Google Sheets (por ejemplo "Resultados CyberTura").
 *  2. Menú Extensiones > Apps Script. Borra lo que haya y pega todo este archivo.
 *  3. Cambia CLAVE por una palabra secreta tuya (la misma va en local.properties).
 *  4. Implementar > Nueva implementación > tipo "Aplicación web".
 *       Ejecutar como: Yo.  Quién tiene acceso: Cualquier persona.
 *     Autoriza los permisos que pide y copia la URL que termina en /exec.
 *  5. En local.properties del proyecto agrega:
 *       cybertura.sheets.url=LA_URL_QUE_COPIASTE
 *       cybertura.sheets.token=LA_MISMA_CLAVE
 *     y vuelve a compilar la app.
 *
 * Cada envío crea o actualiza las pestañas Resumen, Retos, Conocimientos, Encuesta y Guía.
 * Las filas de una sesión que ya estaba se reemplazan, así que enviar varias veces (o desde
 * varios teléfonos) no repite datos.
 */

const CLAVE = 'cambia-esta-clave';
const COLUMNA_SESION = 'ID de sesión';

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000); // dos teléfonos enviando a la vez no se pisan
  try {
    const datos = JSON.parse(e.postData.contents);
    if (datos.token !== CLAVE) return responder({ ok: false, error: 'La clave no coincide con la del script.' });
    const libro = SpreadsheetApp.getActiveSpreadsheet();
    let filas = 0;
    datos.hojas.forEach(function (hoja) { filas += escribirHoja(libro, hoja); });
    return responder({ ok: true, filas: filas });
  } catch (err) {
    return responder({ ok: false, error: String(err) });
  } finally {
    lock.releaseLock();
  }
}

function escribirHoja(libro, hoja) {
  const pestana = libro.getSheetByName(hoja.nombre) || libro.insertSheet(hoja.nombre);
  const ancho = hoja.encabezado.length;

  pestana.getRange(1, 1, 1, ancho).setValues([hoja.encabezado])
    .setFontWeight('bold').setFontColor('#ffffff').setBackground('#1f4e79')
    .setWrap(true).setVerticalAlignment('middle');
  pestana.setFrozenRows(1);

  const colSesion = hoja.encabezado.indexOf(COLUMNA_SESION);
  const ultima = pestana.getLastRow();
  if (colSesion < 0) {
    // Pestaña sin sesiones (la Guía): se reescribe entera
    if (ultima > 1) pestana.getRange(2, 1, ultima - 1, pestana.getMaxColumns()).clearContent();
  } else if (ultima > 1) {
    // Quita las filas de las sesiones que llegan, para no duplicarlas
    const llegan = new Set(hoja.filas.map(function (f) { return String(f[colSesion]); }));
    const ids = pestana.getRange(2, colSesion + 1, ultima - 1, 1).getValues();
    for (let i = ids.length - 1; i >= 0; i--) {
      if (llegan.has(String(ids[i][0]))) pestana.deleteRow(i + 2);
    }
  }

  if (hoja.filas.length > 0) {
    const valores = hoja.filas.map(function (f) { return f.map(valor); });
    pestana.getRange(pestana.getLastRow() + 1, 1, valores.length, ancho).setValues(valores);
  }
  if (colSesion < 0) {
    pestana.setColumnWidth(1, 900);
    pestana.getRange(1, 1, pestana.getMaxRows(), 1).setWrap(true);
  } else {
    pestana.autoResizeColumns(1, ancho);
    // Las preguntas largas del encabezado se leen en varias líneas, no en columnas enormes
    for (let c = 1; c <= ancho; c++) {
      if (pestana.getColumnWidth(c) > 260) pestana.setColumnWidth(c, 260);
    }
  }
  return hoja.filas.length;
}

/** Los números van como números (para promedios); el resto como texto tal cual. */
function valor(v) {
  if (v === '') return '';
  if (/^-?(0|[1-9]\d*)(\.\d+)?$/.test(v)) return Number(v);
  return "'" + v; // evita que Sheets convierta "01" en 1 o una fecha a otro formato
}

function responder(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
