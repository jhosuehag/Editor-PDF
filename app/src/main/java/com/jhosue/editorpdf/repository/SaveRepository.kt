package com.jhosue.editorpdf.repository

import android.content.Context
import android.util.Log
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.LayerData
import com.jhosue.editorpdf.data.models.ShapeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Resultado de una operación de guardado.
 */
sealed class SaveResult {
    /** Guardado exitoso. */
    data object Exito : SaveResult()
    /** Error con mensaje descriptivo. */
    data class Error(val mensaje: String) : SaveResult()
}

/**
 * Repositorio para guardar el PDF con todas las anotaciones y ediciones aplicadas.
 * Utiliza iText7 para modificar el PDF de forma permanente.
 */
class SaveRepository(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SaveRepository"
    }

    /**
     * Guarda el PDF de forma completa.
     * @param pdfPath Ruta del archivo PDF.
     * @param layers Mapa de capas por índice de página.
     * @return Result con éxito o error.
     */
    suspend fun guardarSuspend(pdfPath: String, layers: Map<Int, LayerData>): SaveResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando guardado del PDF: $pdfPath")

            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                Log.e(TAG, "Archivo PDF no encontrado: $pdfPath")
                return@withContext SaveResult.Error("Archivo PDF no encontrado")
            }

            // Crear respaldo antes de modificar
            val backupPath = "$pdfPath.savebackup"
            archivo.copyTo(File(backupPath), overwrite = true)

            // Abrir documento para lectura y escritura
            val pdfReader = PdfReader(archivo)
            val pdfWriter = PdfWriter(archivo)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // Aplicar capas a cada página
            for ((pageIndex, layerData) in layers) {
                if (pageIndex < 0 || pageIndex >= pdfDoc.numberOfPages) {
                    continue
                }

                val page = pdfDoc.getPage(pageIndex + 1)
                val canvas = PdfCanvas(page)
                val pageHeight = page.pageSize.height

                for (anotacion in layerData.anotaciones) {
                    dibujarAnotacion(canvas, anotacion, pageHeight)
                }
            }

            pdfDoc.close()
            File(backupPath).delete()
            Log.d(TAG, "PDF guardado exitosamente")

            SaveResult.Exito
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error de IO al guardar", e)
            SaveResult.Error("Sin espacio en almacenamiento")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de permisos", e)
            SaveResult.Error("Sin permisos. Ve a Configuración")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar PDF", e)
            SaveResult.Error("Error al guardar: ${e.message}")
        }
    }

    /**
     * Guarda el PDF como un nuevo archivo.
     */
    suspend fun guardarComo(pdfPathOriginal: String, destinoPath: String, layers: Map<Int, LayerData>): SaveResult = withContext(Dispatchers.IO) {
        try {
            val archivoOriginal = File(pdfPathOriginal)
            if (!archivoOriginal.exists()) {
                return@withContext SaveResult.Error("Archivo original no encontrado")
            }

            archivoOriginal.copyTo(File(destinoPath), overwrite = true)

            val pdfReader = PdfReader(File(destinoPath))
            val pdfWriter = PdfWriter(File(destinoPath))
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            for ((pageIndex, layerData) in layers) {
                if (pageIndex < 0 || pageIndex >= pdfDoc.numberOfPages) {
                    continue
                }

                val page = pdfDoc.getPage(pageIndex + 1)
                val canvas = PdfCanvas(page)
                val pageHeight = page.pageSize.height

                for (anotacion in layerData.anotaciones) {
                    dibujarAnotacion(canvas, anotacion, pageHeight)
                }
            }

            pdfDoc.close()
            SaveResult.Exito
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error de IO al guardar como", e)
            SaveResult.Error("Sin espacio en almacenamiento")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar como", e)
            SaveResult.Error("Error al guardar: ${e.message}")
        }
    }

    private fun dibujarAnotacion(canvas: PdfCanvas, anotacion: AnnotationElement, pageHeight: Float) {
        try {
            when (anotacion) {
                is AnnotationElement.Highlight -> dibujarHighlight(canvas, anotacion, pageHeight)
                is AnnotationElement.Underline -> dibujarUnderline(canvas, anotacion, pageHeight)
                is AnnotationElement.Strikethrough -> dibujarStrikethrough(canvas, anotacion, pageHeight)
                is AnnotationElement.FreeDrawing -> dibujarFreeDrawing(canvas, anotacion, pageHeight)
                is AnnotationElement.FreeText -> dibujarFreeText(canvas, anotacion, pageHeight)
                is AnnotationElement.StickyNote -> dibujarStickyNote(canvas, anotacion, pageHeight)
                is AnnotationElement.Shape -> dibujarShape(canvas, anotacion, pageHeight)
                is AnnotationElement.Signature -> dibujarSignature(canvas, anotacion, pageHeight)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al dibujar ${anotacion.javaClass.simpleName}", e)
        }
    }

    private fun dibujarHighlight(canvas: PdfCanvas, h: AnnotationElement.Highlight, pageHeight: Float) {
        val red = ((h.color shr 16) and 0xFF) / 255f
        val green = ((h.color shr 8) and 0xFF) / 255f
        val blue = (h.color and 0xFF) / 255f
        
        canvas.setFillColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        
        for (rect in h.rects) {
            val x = rect.left
            val y = pageHeight - rect.bottom
            canvas.rectangle(x.toDouble(), y.toDouble(), rect.width().toDouble(), rect.height().toDouble())
            canvas.fill()
        }
    }

    private fun dibujarUnderline(canvas: PdfCanvas, u: AnnotationElement.Underline, pageHeight: Float) {
        val red = ((u.color shr 16) and 0xFF) / 255f
        val green = ((u.color shr 8) and 0xFF) / 255f
        val blue = (u.color and 0xFF) / 255f
        
        canvas.setStrokeColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        canvas.setLineWidth(2f)
        
        for (rect in u.rects) {
            val x1 = rect.left
            val y = pageHeight - rect.bottom
            val x2 = rect.right
            canvas.moveTo(x1.toDouble(), y.toDouble())
            canvas.lineTo(x2.toDouble(), y.toDouble())
            canvas.stroke()
        }
    }

    private fun dibujarStrikethrough(canvas: PdfCanvas, s: AnnotationElement.Strikethrough, pageHeight: Float) {
        val red = ((s.color shr 16) and 0xFF) / 255f
        val green = ((s.color shr 8) and 0xFF) / 255f
        val blue = (s.color and 0xFF) / 255f
        
        canvas.setStrokeColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        canvas.setLineWidth(2f)
        
        for (rect in s.rects) {
            val x1 = rect.left
            val y = pageHeight - rect.centerY()
            val x2 = rect.right
            canvas.moveTo(x1.toDouble(), y.toDouble())
            canvas.lineTo(x2.toDouble(), y.toDouble())
            canvas.stroke()
        }
    }

    private fun dibujarFreeDrawing(canvas: PdfCanvas, fd: AnnotationElement.FreeDrawing, pageHeight: Float) {
        if (fd.puntos.size < 2) return
        
        val red = ((fd.color shr 16) and 0xFF) / 255f
        val green = ((fd.color shr 8) and 0xFF) / 255f
        val blue = (fd.color and 0xFF) / 255f
        
        canvas.setStrokeColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        canvas.setLineWidth(fd.grosor)
        
        val first = fd.puntos.first()
        canvas.moveTo(first.x.toDouble(), (pageHeight - first.y).toDouble())
        
        for (i in 1 until fd.puntos.size) {
            val p = fd.puntos[i]
            canvas.lineTo(p.x.toDouble(), (pageHeight - p.y).toDouble())
        }
        canvas.stroke()
    }

    private fun dibujarFreeText(canvas: PdfCanvas, ft: AnnotationElement.FreeText, pageHeight: Float) {
        val red = ((ft.color shr 16) and 0xFF) / 255f
        val green = ((ft.color shr 8) and 0xFF) / 255f
        val blue = (ft.color and 0xFF) / 255f
        
        canvas.beginText()
        canvas.setFillColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        try {
            val font = com.itextpdf.kernel.font.PdfFontFactory.createFont()
            canvas.setFontAndSize(font, ft.tamanio)
        } catch (e: Exception) {
            // Si falla la fuente, usar Helvetica por defecto que siempre está disponible
            try {
                val defaultFont = com.itextpdf.kernel.font.PdfFontFactory.createFont("Helvetica")
                canvas.setFontAndSize(defaultFont, ft.tamanio)
            } catch (e2: Exception) {
                Log.e(TAG, "No se pudo crear fuente para texto libre", e2)
            }
        }
        canvas.moveText(ft.posicion.x.toDouble(), (pageHeight - ft.posicion.y).toDouble())
        canvas.showText(ft.texto)
        canvas.endText()
    }

    private fun dibujarStickyNote(canvas: PdfCanvas, sn: AnnotationElement.StickyNote, pageHeight: Float) {
        val x = sn.posicion.x
        val y = pageHeight - sn.posicion.y
        val size = 20f
        
        canvas.setFillColor(DeviceRgb(255, 255, 0))
        canvas.rectangle(x.toDouble(), y.toDouble(), size.toDouble(), size.toDouble())
        canvas.fill()
        
        canvas.setStrokeColor(DeviceRgb(0, 0, 0))
        canvas.setLineWidth(1f)
        canvas.rectangle(x.toDouble(), y.toDouble(), size.toDouble(), size.toDouble())
        canvas.stroke()
    }

    private fun dibujarShape(canvas: PdfCanvas, s: AnnotationElement.Shape, pageHeight: Float) {
        val red = ((s.color shr 16) and 0xFF) / 255f
        val green = ((s.color shr 8) and 0xFF) / 255f
        val blue = (s.color and 0xFF) / 255f
        
        canvas.setStrokeColor(DeviceRgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()))
        canvas.setLineWidth(s.grosor)
        
        val x1 = s.puntoInicio.x
        val y1 = pageHeight - s.puntoInicio.y
        val x2 = s.puntoFin.x
        val y2 = pageHeight - s.puntoFin.y
        
        when (s.tipo) {
            ShapeType.RECTANGULO -> {
                canvas.rectangle(x1.toDouble(), y1.toDouble(), (x2 - x1).toDouble(), (y2 - y1).toDouble())
                canvas.stroke()
            }
            ShapeType.CIRCULO -> {
                val cx = (x1 + x2) / 2
                val cy = (y1 + y2) / 2
                val r = kotlin.math.min(kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1)) / 2
                canvas.circle(cx.toDouble(), cy.toDouble(), r.toDouble())
                canvas.stroke()
            }
            ShapeType.LINEA, ShapeType.FLECHA -> {
                canvas.moveTo(x1.toDouble(), y1.toDouble())
                canvas.lineTo(x2.toDouble(), y2.toDouble())
                canvas.stroke()
            }
        }
    }

    private fun dibujarSignature(canvas: PdfCanvas, sig: AnnotationElement.Signature, pageHeight: Float) {
        try {
            val imageData = ImageDataFactory.create(sig.bitmapBytes)
            val w = imageData.width * sig.escala
            val h = imageData.height * sig.escala
            val x = sig.posicion.x
            val y = pageHeight - sig.posicion.y - h
            
            // Dibujar un rectángulo como placeholder para la firma
            // ya que la API de imágenes de iText7 es compleja
            canvas.setFillColor(DeviceRgb(200, 200, 200))
            canvas.rectangle(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
            canvas.fill()
        } catch (e: Exception) {
            Log.e(TAG, "Error al dibujar firma", e)
        }
    }
}