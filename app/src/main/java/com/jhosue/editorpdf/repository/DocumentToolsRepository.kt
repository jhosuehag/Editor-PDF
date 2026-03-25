package com.jhosue.editorpdf.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.utils.PdfMerger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Repositorio para manejar operaciones de herramientas de documento PDF.
 * Proporciona métodos para unir, dividir y convertir imágenes a PDF.
 */
class DocumentToolsRepository {

    companion object {
        private const val TAG = "DocumentToolsRepository"
    }

    /**
     * Une múltiples archivos PDF en uno solo.
     * @param uris Lista de URIs de los archivos PDF a unir.
     * @param context Contexto de la aplicación.
     * @param nombreDestino Nombre del archivo PDF resultante.
     * @param onProgreso Callback para reportar progreso (0.0 a 1.0).
     * @return Result con la ruta del archivo creado o error.
     */
    suspend fun unirPdfs(
        uris: List<Uri>,
        context: Context,
        nombreDestino: String,
        onProgreso: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Uniendo ${uris.size} archivos PDF")

            if (uris.size < 2) {
                return@withContext Result.failure(Exception("Se necesitan al menos 2 archivos para unir"))
            }

            // Crear directorio de destino si no existe
            val directorio = context.getExternalFilesDir(null)
            if (directorio == null) {
                return@withContext Result.failure(Exception("No se pudo acceder al directorio de archivos"))
            }

            val archivoDestino = File(directorio, "$nombreDestino.pdf")
            val archivosTemporales = mutableListOf<File>()

            onProgreso(0.1f)

            // Copiar cada URI a un archivo temporal
            uris.forEachIndexed { index, uri ->
                val archivoTemp = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}_$index.pdf")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(archivoTemp).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext Result.failure(Exception("No se pudo leer el archivo ${index + 1}"))
                archivosTemporales.add(archivoTemp)
                onProgreso(0.1f + (0.3f * (index + 1) / uris.size))
            }

            onProgreso(0.4f)

            // Crear documento PDF de salida
            val pdfWriter = PdfWriter(archivoDestino)
            val pdfDoc = PdfDocument(pdfWriter)
            val merger = PdfMerger(pdfDoc)

            // Fusionar cada archivo temporal
            archivosTemporales.forEachIndexed { index, archivoTemp ->
                val pdfReader = PdfReader(archivoTemp)
                val srcDoc = PdfDocument(pdfReader)
                merger.merge(srcDoc, 1, srcDoc.numberOfPages)
                srcDoc.close()
                onProgreso(0.4f + (0.5f * (index + 1) / archivosTemporales.size))
            }

            pdfDoc.close()

            // Limpiar archivos temporales
            archivosTemporales.forEach { it.delete() }

            onProgreso(1.0f)
            Log.d(TAG, "PDFs unidos exitosamente: ${archivoDestino.absolutePath}")
            Result.success(archivoDestino.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error al unir PDFs", e)
            Result.failure(e)
        }
    }

    /**
     * Divide un PDF extrayendo un rango de páginas.
     * @param pdfPath Ruta del archivo PDF a dividir.
     * @param rangoDesde Página inicial del rango (1-based).
     * @param rangoHasta Página final del rango (1-based).
     * @param nombreBase Nombre base para el archivo de salida.
     * @param onProgreso Callback para reportar progreso.
     * @return Result con la ruta del archivo creado o error.
     */
    suspend fun dividirPdf(
        pdfPath: String,
        rangoDesde: Int,
        rangoHasta: Int,
        nombreBase: String,
        onProgreso: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Dividiendo PDF: páginas $rangoDesde a $rangoHasta")

            val archivoOrigen = File(pdfPath)
            if (!archivoOrigen.exists()) {
                return@withContext Result.failure(Exception("Archivo PDF no encontrado"))
            }

            val directorio = archivoOrigen.parentFile
            val archivoDestino = File(directorio, "${nombreBase}_pags_${rangoDesde}-${rangoHasta}.pdf")

            onProgreso(0.2f)

            // Abrir documento origen
            val pdfReader = PdfReader(archivoOrigen)
            val srcDoc = PdfDocument(pdfReader)

            // Validar rango
            if (rangoDesde < 1 || rangoHasta > srcDoc.numberOfPages || rangoDesde > rangoHasta) {
                srcDoc.close()
                return@withContext Result.failure(Exception("Rango de páginas inválido"))
            }

            onProgreso(0.4f)

            // Crear documento de salida
            val pdfWriter = PdfWriter(archivoDestino)
            val destDoc = PdfDocument(pdfWriter)

            // Copiar páginas del rango
            for (i in rangoDesde..rangoHasta) {
                destDoc.addPage(srcDoc.getPage(i))
                onProgreso(0.4f + (0.5f * (i - rangoDesde + 1) / (rangoHasta - rangoDesde + 1)))
            }

            destDoc.close()
            srcDoc.close()

            onProgreso(1.0f)
            Log.d(TAG, "PDF dividido exitosamente: ${archivoDestino.absolutePath}")
            Result.success(archivoDestino.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error al dividir PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Divide un PDF en partes iguales.
     * @param pdfPath Ruta del archivo PDF a dividir.
     * @param nPartes Número de partes en que dividir.
     * @param nombreBase Nombre base para los archivos de salida.
     * @param onProgreso Callback para reportar progreso.
     * @return Result con la lista de rutas de archivos creados o error.
     */
    suspend fun dividirEnPartes(
        pdfPath: String,
        nPartes: Int,
        nombreBase: String,
        onProgreso: (Float) -> Unit = {}
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Dividiendo PDF en $nPartes partes")

            val archivoOrigen = File(pdfPath)
            if (!archivoOrigen.exists()) {
                return@withContext Result.failure(Exception("Archivo PDF no encontrado"))
            }

            if (nPartes < 2) {
                return@withContext Result.failure(Exception("Se necesitan al menos 2 partes"))
            }

            val directorio = archivoOrigen.parentFile

            // Abrir documento origen
            val pdfReader = PdfReader(archivoOrigen)
            val srcDoc = PdfDocument(pdfReader)
            val totalPaginas = srcDoc.numberOfPages

            val paginasPorParte = totalPaginas / nPartes
            val paginasExtra = totalPaginas % nPartes

            val archivosCreados = mutableListOf<String>()
            var paginaActual = 1

            for (parte in 0 until nPartes) {
                val desde = paginaActual
                val hasta = if (parte < paginasExtra) {
                    paginaActual + paginasPorParte
                } else {
                    paginaActual + paginasPorParte - 1
                }
                paginaActual = hasta + 1

                val archivoDestino = File(directorio, "${nombreBase}_parte${parte + 1}.pdf")
                val pdfWriter = PdfWriter(archivoDestino)
                val destDoc = PdfDocument(pdfWriter)

                for (i in desde..hasta) {
                    destDoc.addPage(srcDoc.getPage(i))
                }

                destDoc.close()
                archivosCreados.add(archivoDestino.absolutePath)
                onProgreso((parte + 1).toFloat() / nPartes)
            }

            srcDoc.close()

            Log.d(TAG, "PDF dividido en $nPartes partes exitosamente")
            Result.success(archivosCreados)
        } catch (e: Exception) {
            Log.e(TAG, "Error al dividir PDF en partes", e)
            Result.failure(e)
        }
    }

    /**
     * Convierte una lista de imágenes a un archivo PDF.
     * @param uris Lista de URIs de las imágenes.
     * @param context Contexto de la aplicación.
     * @param orientacion Orientación de las páginas: "VERTICAL" u "HORIZONTAL".
     * @param margen Valores de margen: "NINGUNO" (0), "PEQUENO" (10), "NORMAL" (20).
     * @param nombreDestino Nombre del archivo PDF resultante.
     * @param onProgreso Callback para reportar progreso.
     * @return Result con la ruta del archivo creado o error.
     */
    suspend fun imagenesToPdf(
        uris: List<Uri>,
        context: Context,
        orientacion: String,
        margen: Float,
        nombreDestino: String,
        onProgreso: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Convirtiendo ${uris.size} imágenes a PDF")

            if (uris.isEmpty()) {
                return@withContext Result.failure(Exception("No hay imágenes para convertir"))
            }

            val directorio = context.getExternalFilesDir(null)
            if (directorio == null) {
                return@withContext Result.failure(Exception("No se pudo acceder al directorio de archivos"))
            }

            val archivoDestino = File(directorio, "$nombreDestino.pdf")

            // Dimensiones de página A4
            val pageWidth = if (orientacion == "HORIZONTAL") PageSize.A4.height else PageSize.A4.width
            val pageHeight = if (orientacion == "HORIZONTAL") PageSize.A4.width else PageSize.A4.height

            // Crear documento PDF
            val pdfWriter = PdfWriter(archivoDestino)
            val pdfDoc = PdfDocument(pdfWriter)

            uris.forEachIndexed { index, uri ->
                // Cargar bitmap de la imagen
                val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: return@withContext Result.failure(Exception("No se pudo cargar imagen ${index + 1}"))

                if (bitmap.width == 0 || bitmap.height == 0) {
                    return@withContext Result.failure(Exception("Imagen ${index + 1} tiene dimensiones inválidas"))
                }

                // Calcular dimensiones escaladas para mantener aspecto
                val margenTotal = margen
                val areaDisponibleAncho = pageWidth - (2 * margenTotal)
                val areaDisponibleAlto = pageHeight - (2 * margenTotal)

                val escalaAncho = areaDisponibleAncho / bitmap.width
                val escalaAlto = areaDisponibleAlto / bitmap.height
                val escala = minOf(escalaAncho, escalaAlto)

                val anchoFinal = bitmap.width * escala
                val altoFinal = bitmap.height * escala

                // Centrar imagen en la página
                val x = (pageWidth - anchoFinal) / 2
                val y = (pageHeight - altoFinal) / 2

                // Crear página nueva
                val pageSize = if (orientacion == "HORIZONTAL") {
                    PageSize(pageWidth, pageHeight)
                } else {
                    PageSize(pageWidth, pageHeight)
                }
                val page = pdfDoc.addNewPage(pageSize)

                // Convertir bitmap a bytes
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val imageBytes = outputStream.toByteArray()

                // Crear imagen en el PDF
                val imageData = ImageDataFactory.create(imageBytes)
                val canvas = PdfCanvas(page)

                canvas.saveState()
                canvas.addImageFittedIntoRectangle(
                    imageData,
                    com.itextpdf.kernel.geom.Rectangle(x, y, anchoFinal, altoFinal),
                    false
                )
                canvas.restoreState()

                bitmap.recycle()
                onProgreso((index + 1).toFloat() / uris.size)
            }

            pdfDoc.close()

            Log.d(TAG, "Imágenes convertidas a PDF exitosamente: ${archivoDestino.absolutePath}")
            Result.success(archivoDestino.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error al convertir imágenes a PDF", e)
            Result.failure(e)
        }
    }
}