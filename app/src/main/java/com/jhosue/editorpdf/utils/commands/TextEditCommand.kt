package com.jhosue.editorpdf.utils.commands

import android.util.Log
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.jhosue.editorpdf.data.models.ContentEdit
import com.jhosue.editorpdf.repository.LayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Comando para editar texto en el PDF.
 * Permite deshacer la edición restaurando el texto original.
 * @param edit Edición de texto a aplicar.
 * @param layerRepository Repositorio de layers.
 * @param layerData Flow del layer actual.
 * @param pdfPath Ruta del archivo PDF.
 */
class TextEditCommand(
    private val edit: ContentEdit.TextEdit,
    private val layerRepository: LayerRepository,
    private val layerData: MutableStateFlow<com.jhosue.editorpdf.data.models.LayerData>,
    private val pdfPath: String
) : EditCommand {
    
    override val descripcion: String = "Editar texto"

    /**
     * Ejecuta el comando: aplica la edición de texto al PDF.
     */
    override suspend fun execute() {
        aplicarTextEdit(edit)
    }

    /**
     * Deshace el comando: revierte el texto original restaurando el texto anterior.
     */
    override suspend fun undo() {
        // Crear edición inversa: nuevo texto se convierte en original, y original vuelve
        val editInverso = ContentEdit.TextEdit(
            id = java.util.UUID.randomUUID().toString(),
            pageIndex = edit.pageIndex,
            textoOriginal = edit.textoNuevo, // El texto que estaba ahora es el "original"
            textoNuevo = edit.textoOriginal,   // Volvemos al texto original
            posicionRect = edit.posicionRect,
            fuente = edit.fuente,
            tamanio = edit.tamanio,
            color = edit.color,
            negrita = edit.negrita,
            cursiva = edit.cursiva,
            alineacion = edit.alineacion
        )
        aplicarTextEdit(editInverso)
    }

    /**
     * Aplica una edición de texto al PDF usando iText7.
     */
    private fun aplicarTextEdit(textEdit: ContentEdit.TextEdit) {
        try {
            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                Log.e("TextEditCommand", "Archivo PDF no encontrado: $pdfPath")
                return
            }

            // Crear respaldo
            val archivoBackup = File("$pdfPath.undobackup")
            archivo.copyTo(archivoBackup, overwrite = true)

            // Abrir el documento
            val pdfReader = PdfReader(archivo)
            val pdfWriter = PdfWriter(archivo)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // Obtener la página (iText usa 1-based)
            val pagina = pdfDoc.getPage(textEdit.pageIndex + 1)
            val pageHeight = pagina.pageSize.height

            // Crear canvas para dibujar
            val canvas = PdfCanvas(pagina)

            // Convertir Y de coordenadas Android (arriba-izquierda) a iText (abajo-izquierda)
            val x = textEdit.posicionRect.left
            val y = pageHeight - textEdit.posicionRect.bottom

            // Dibujar rectángulo blanco sobre el área del texto
            canvas.setFillColor(DeviceRgb(255, 255, 255))
            canvas.rectangle(
                x.toDouble(),
                y.toDouble(),
                textEdit.posicionRect.width().toDouble(),
                textEdit.posicionRect.height().toDouble()
            )
            canvas.fill()

            // Crear fuente
            val font = crearFuente(textEdit.fuente, textEdit.negrita, textEdit.cursiva)

            // Convertir color ARGB a iText
            val red = (textEdit.color shr 16) and 0xFF
            val green = (textEdit.color shr 8) and 0xFF
            val blue = textEdit.color and 0xFF
            val textColor = DeviceRgb(red, green, blue)

            // Establecer color y fuente
            canvas.setFontAndSize(font, textEdit.tamanio)
            canvas.setFillColor(textColor)

            // Escribir el texto
            val lineas = textEdit.textoNuevo.split("\n")
            var yActual = y
            val alturaLinea = textEdit.tamanio * 1.2f

            for (linea in lineas) {
                val xAlineado = calcularXAlineado(linea, textEdit.posicionRect, textEdit.tamanio, textEdit.alineacion)
                canvas.beginText()
                canvas.moveText(xAlineado.toDouble(), yActual.toDouble())
                canvas.showText(linea)
                canvas.endText()
                yActual -= alturaLinea
            }

            pdfDoc.close()
            Log.d("TextEditCommand", "TextEdit aplicado exitosamente")

        } catch (e: Exception) {
            Log.e("TextEditCommand", "Error al aplicar TextEdit", e)
            // Restaurar del backup
            val archivo = File(pdfPath)
            val backup = File("$pdfPath.undobackup")
            if (backup.exists()) {
                backup.copyTo(archivo, overwrite = true)
                backup.delete()
            }
        }
    }

    /**
     * Crea una fuente PDF según el nombre, negrita y cursiva.
     */
    private fun crearFuente(fuenteNombre: String, negrita: Boolean, cursiva: Boolean): com.itextpdf.kernel.font.PdfFont {
        val fuenteBase = when {
            fuenteNombre.contains("Times", ignoreCase = true) -> "Times"
            fuenteNombre.contains("Courier", ignoreCase = true) -> "Courier"
            else -> "Helvetica"
        }

        val sufijo = when {
            negrita && cursiva -> "BoldOblique"
            negrita -> "Bold"
            cursiva -> "Oblique"
            else -> ""
        }

        val nombreCompleto = "$fuenteBase$sufijo"
        return try {
            PdfFontFactory.createFont(nombreCompleto)
        } catch (e: Exception) {
            try {
                PdfFontFactory.createFont(fuenteBase)
            } catch (e2: Exception) {
                PdfFontFactory.createFont("Helvetica")
            }
        }
    }

    /**
     * Calcula la posición X para la alineación del texto.
     */
    private fun calcularXAlineado(texto: String, rect: android.graphics.RectF, tamanio: Float, alineacion: String): Float {
        val anchoTexto = texto.length * tamanio * 0.6f
        val anchoDisponible = rect.width()

        return when (alineacion.uppercase()) {
            "CENTER" -> rect.left + (anchoDisponible - anchoTexto) / 2
            "RIGHT" -> rect.right - anchoTexto
            else -> rect.left
        }.coerceAtLeast(rect.left)
    }
}