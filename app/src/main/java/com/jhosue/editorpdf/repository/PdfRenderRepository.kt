package com.jhosue.editorpdf.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import com.jhosue.editorpdf.data.models.PdfState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Repositorio para manejar la apertura y renderizado de PDFs usando PdfRenderer.
 * Proporciona métodos para abrir documentos, renderizar páginas y gestionar recursos.
 * 
 * OPTIMIZACIÓN: Incluye caché LruCache para almacenar bitmaps de páginas ya renderizadas.
 * Esto reduce significativamente el uso de memoria y mejora el rendimiento al navegar
 * entre páginas previamente visitadas, evitando re-renderizados innecesarios.
 */
class PdfRenderRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "PdfRenderRepository"
    }

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var currentFile: File? = null
    private var totalPages: Int = 0

    /**
     * Caché LRU para almacenar bitmaps de páginas renderizadas.
     * Usa el 25% de la memoria máxima disponible de la JVM.
     * Esto evita re-renderizados al navegar entre páginas ya visitadas,
     * reduciendo uso de CPU y mejorando fluidez de scroll.
     */
    private val cache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()
    ) {
        /**
         * Called when a cached bitmap is evicted.
         * Recicla el bitmap para liberar memoria nativa inmediatamente.
         */
        override fun entryRemoved(
            evicted: Boolean,
            key: Int?,
            oldValue: Bitmap?,
            newValue: Bitmap?
        ) {
            if (evicted && oldValue != null && !oldValue.isRecycled) {
                // Reciclar bitmap evictado para liberar memoria nativa
                oldValue.recycle()
            }
        }
    }

    /**
     * Obtiene un bitmap del caché o null si no existe.
     * @param pageIndex Índice de la página.
     * @return Bitmap en caché o null.
     */
    fun getCachedBitmap(pageIndex: Int): Bitmap? {
        return cache.get(pageIndex)
    }

    /**
     * Verifica si una página está en caché.
     * @param pageIndex Índice de la página.
     * @return true si está en caché.
     */
    fun isPageCached(pageIndex: Int): Boolean {
        return cache.get(pageIndex) != null
    }

    /**
     * Abre un PDF desde una URI.
     * Maneja diferentes tipos de errores como archivos no encontrados,
     * PDFs protegidos con contraseña, archivos corruptos o sin permisos.
     * @param uri URI del archivo PDF a abrir.
     * @return PdfState con el resultado de la operación.
     */
    suspend fun openPdf(uri: Uri): PdfState = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Abriendo PDF desde URI: $uri")

            // Cerrar PDF anterior si existe (incluye limpiar caché)
            closePdf()

            // Usar applicationContext para evitar memory leaks con Activity context
            val appContext = context.applicationContext ?: context

            // Copiar el archivo a un archivo temporal para PdfRenderer
            val tempFile = File(appContext.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
            currentFile = tempFile

            // Intentar abrir el input stream y copiar el archivo
            val inputStream = try {
                appContext.contentResolver.openInputStream(uri)
            } catch (e: SecurityException) {
                Log.e(TAG, "Sin permisos para leer la URI", e)
                return@withContext PdfState.Error("Sin permisos para leer este archivo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al abrir el archivo", e)
                return@withContext PdfState.Error("No se pudo leer el archivo PDF")
            }

            if (inputStream == null) {
                Log.e(TAG, "InputStream es null")
                return@withContext PdfState.Error("No se pudo leer el archivo PDF")
            }

            try {
                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error al copiar el archivo a cache", e)
                return@withContext PdfState.Error("No se pudo procesar el archivo PDF")
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al copiar archivo", e)
                return@withContext PdfState.Error("Error al procesar el archivo PDF")
            }

            // Verificar que el archivo fue copiado correctamente
            if (!tempFile.exists() || tempFile.length() == 0L) {
                Log.e(TAG, "El archivo copiado está vacío o no existe")
                return@withContext PdfState.Error("No se pudo copiar el archivo PDF")
            }

            // Obtener el descriptor del archivo
            val pfd = try {
                ParcelFileDescriptor.open(
                    tempFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            } catch (e: IOException) {
                Log.e(TAG, "Error al abrir el descriptor de archivo", e)
                return@withContext PdfState.Error("El archivo PDF no es válido o está dañado")
            } catch (e: SecurityException) {
                Log.e(TAG, "Sin permisos para leer el archivo", e)
                return@withContext PdfState.Error("Sin permisos para leer este archivo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al abrir archivo como PDF", e)
                return@withContext PdfState.Error("El archivo PDF no es válido o está dañado")
            }

            parcelFileDescriptor = pfd

            // Crear PdfRenderer usando el descriptor
            val renderer = try {
                PdfRenderer(pfd)
            } catch (e: IOException) {
                Log.e(TAG, "Error al crear PdfRenderer - archivo corrupto", e)
                return@withContext PdfState.Error("El archivo PDF está corrupto o no es válido")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "PDF protegido o formato no válido", e)
                return@withContext PdfState.Error("Este PDF está protegido o no es válido")
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al crear PdfRenderer", e)
                return@withContext PdfState.Error("El archivo PDF no es válido o está dañado")
            }

            pdfRenderer = renderer
            totalPages = renderer.pageCount

            // Obtener nombre del archivo sin extensión de forma segura
            val fileName = tempFile.nameWithoutExtension.ifEmpty { "documento" }
            Log.d(TAG, "PDF abierto exitosamente. Páginas: $totalPages")

            PdfState.Success(
                totalPages = totalPages,
                fileName = fileName
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permisos para leer el archivo", e)
            PdfState.Error("Sin permisos para leer este archivo")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "PDF protegido o formato no válido", e)
            PdfState.Error("Este PDF está protegido o no es válido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir el PDF", e)
            PdfState.Error("El archivo PDF no es válido o está dañado")
        }
    }

    /**
     * Resultado del renderizado de una página.
     * @property bitmap Bitmap renderizado.
     * @property originalWidth Ancho original de la página PDF en puntos.
     * @property originalHeight Alto original de la página PDF en puntos.
     */
    data class RenderResult(
        val bitmap: Bitmap,
        val originalWidth: Int,
        val originalHeight: Int
    )

    /**
     * Renderiza una página específica del PDF a un Bitmap.
     * Primero verifica si la página está en caché para evitar re-renderizado.
     * Después de renderizar, guarda el resultado en caché para uso futuro.
     * 
     * @param pageIndex Índice de la página a renderizar (0-based).
     * @param targetWidth Ancho objetivo en píxeles. El alto se calcula manteniendo la proporción.
     * @return RenderResult con el Bitmap y dimensiones originales, o null si falla.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): RenderResult? = withContext(Dispatchers.IO) {
        try {
            val renderer = pdfRenderer ?: run {
                Log.e(TAG, "PdfRenderer no inicializado")
                return@withContext null
            }

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                Log.e(TAG, "Índice de página inválido: $pageIndex")
                return@withContext null
            }

            // OPTIMIZACIÓN: Verificar si la página ya está en caché
            // Si existe, retornar directamente sin re-renderizar
            cache.get(pageIndex)?.let { cachedBitmap ->
                Log.d(TAG, "Página $pageIndex obtenida del caché")
                // Crear copy del bitmap del caché para evitar problemas de reciclaje
                // si el caché lo evicta mientras se usa
                val bitmapCopy = cachedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                return@withContext RenderResult(
                    bitmap = bitmapCopy,
                    originalWidth = cachedBitmap.width,
                    originalHeight = cachedBitmap.height
                )
            }

            Log.d(TAG, "Renderizando página $pageIndex con ancho $targetWidth")

            // Abrir la página
            val page = renderer.openPage(pageIndex)

            // Guardar las dimensiones originales de la página PDF
            val originalWidth = page.width
            val originalHeight = page.height

            // Calcular el alto manteniendo la proporción
            val scale = targetWidth.toFloat() / page.width
            val targetHeight = (page.height * scale).toInt()

            // Crear bitmap con configuración ARGB_8888 para mejor calidad
            val bitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )

            // Renderizar la página al bitmap usando el rectángulo de destino
            val destRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
            page.render(
                bitmap,
                destRect,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            // Cerrar la página
            page.close()

            // OPTIMIZACIÓN: Guardar en caché para uso futuro
            // Esto mejora significativamente la navegación entre páginas
            cache.put(pageIndex, bitmap)

            Log.d(TAG, "Página $pageIndex renderizada exitosamente y almacenada en caché")
            RenderResult(bitmap, originalWidth, originalHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error al renderizar página $pageIndex", e)
            null
        }
    }

    /**
     * Renderiza una página en segundo plano y la guarda en caché.
     * No retorna ningún resultado ni modifica el estado.
     * Es útil para pre-cargar páginas adyacentes mientras el usuario navega.
     * 
     * @param pageIndex Índice de la página a precargar (0-based).
     * @param targetWidth Ancho objetivo en píxeles.
     */
    suspend fun precargarPagina(pageIndex: Int, targetWidth: Int = 1080) = withContext(Dispatchers.IO) {
        try {
            // Solo precargar si no está ya en caché
            if (cache.get(pageIndex) != null) {
                Log.d(TAG, "Página $pageIndex ya está en caché, omitiendo precarga")
                return@withContext
            }

            val renderer = pdfRenderer ?: return@withContext

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext
            }

            Log.d(TAG, "Precargando página $pageIndex en background")

            val page = renderer.openPage(pageIndex)
            val originalWidth = page.width
            val originalHeight = page.height
            val scale = targetWidth.toFloat() / page.width
            val targetHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )

            val destRect = android.graphics.Rect(0, 0, targetWidth, targetHeight)
            page.render(bitmap, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Guardar en caché
            cache.put(pageIndex, bitmap)
            Log.d(TAG, "Página $pageIndex precargada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al precargar página $pageIndex", e)
        }
    }

    /**
     * Obtiene el número total de páginas del PDF actualmente abierto.
     * @return Total de páginas o 0 si no hay PDF abierto.
     */
    fun getTotalPages(): Int = totalPages

    /**
     * Cierra el PDF y libera todos los recursos asociados.
     * IMPORTANTE: Limpia el caché de páginas para liberar memoria.
     */
    fun closePdf() {
        try {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
            currentFile?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar el PDF", e)
        } finally {
            pdfRenderer = null
            parcelFileDescriptor = null
            currentFile = null
            totalPages = 0
            
            // OPTIMIZACIÓN: Limpiar caché al cerrar
            // Esto libera toda la memoria usada por bitmaps en caché
            cache.evictAll()
        }
    }

    /**
     * Obtiene la ruta del archivo PDF temporal actualmente abierto.
     * @return Ruta absoluta del archivo o null si no hay PDF abierto.
     */
    fun getPdfFilePath(): String? {
        return currentFile?.absolutePath
    }
}