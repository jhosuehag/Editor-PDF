package com.jhosue.editorpdf.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jhosue.editorpdf.repository.DocumentToolsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Estado de operación para dividir PDF.
 */
sealed class DividirPdfEstado {
    /** Estado inicial, sin operación en curso. */
    data object Idle : DividirPdfEstado()
    /** PDF cargado listo para dividir. */
    data class Listo(val totalPaginas: Int, val nombreArchivo: String, val tamanoBytes: Long) : DividirPdfEstado()
    /** Operación en curso con progreso. */
    data class Procesando(val progreso: Float, val mensaje: String) : DividirPdfEstado()
    /** Operación exitosa con lista de archivos creados. */
    data class Exito(val archivosCreados: List<String>) : DividirPdfEstado()
    /** Operación fallida con mensaje de error. */
    data class Error(val mensaje: String) : DividirPdfEstado()
}

/**
 * ViewModel para la pantalla de dividir PDFs.
 * Maneja la carga del PDF y las operaciones de división.
 */
class DividirPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentToolsRepository()

    /** Estado actual de la operación de dividir PDF. */
    private val _estado = MutableStateFlow<DividirPdfEstado>(DividirPdfEstado.Idle)
    val estado: StateFlow<DividirPdfEstado> = _estado.asStateFlow()

    /** URI del archivo PDF actualmente cargado. */
    private val _pdfUri = MutableStateFlow<Uri?>(null)
    val pdfUri: StateFlow<Uri?> = _pdfUri.asStateFlow()

    /** Ruta temporal del archivo PDF copiado a caché. */
    private var pdfPathTemporal: String? = null

    /** Total de páginas del PDF cargado. */
    private val _totalPaginas = MutableStateFlow(0)
    val totalPaginas: StateFlow<Int> = _totalPaginas.asStateFlow()

    /** Nombre del archivo PDF. */
    private val _nombreArchivo = MutableStateFlow("")
    val nombreArchivo: StateFlow<String> = _nombreArchivo.asStateFlow()

    /** Miniaturas de las páginas del PDF. */
    private val _miniaturas = MutableStateFlow<List<Bitmap>>(emptyList())
    val miniaturas: StateFlow<List<Bitmap>> = _miniaturas.asStateFlow()

    /**
     * Carga un archivo PDF desde una URI.
     * @param uri URI del archivo PDF.
     */
    fun cargarPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // Copiar a archivo temporal
                val archivoTemp = File(context.cacheDir, "temp_dividir_${System.currentTimeMillis()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.FileOutputStream(archivoTemp).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        _estado.value = DividirPdfEstado.Error("No se pudo leer el archivo")
                    }
                    return@launch
                }

                pdfPathTemporal = archivoTemp.absolutePath

                // Extraer información del archivo
                val nombre = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (nameIndex >= 0) cursor.getString(nameIndex) else archivoTemp.name
                } ?: archivoTemp.name

                val tamano = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    if (sizeIndex >= 0) cursor.getLong(sizeIndex) else archivoTemp.length()
                } ?: archivoTemp.length()

                // Obtener número de páginas con PdfRenderer
                val parcelFileDescriptor = ParcelFileDescriptor.open(
                    archivoTemp,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                val pdfRenderer = PdfRenderer(parcelFileDescriptor)
                val totalPaginas = pdfRenderer.pageCount

                _totalPaginas.value = totalPaginas
                _nombreArchivo.value = nombre
                _pdfUri.value = uri

                // Generar miniaturas
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until totalPaginas) {
                    val page = pdfRenderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        page.width / 4,
                        page.height / 4,
                        Bitmap.Config.ARGB_8888
                    )
                    val destRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                    page.render(bitmap, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
                pdfRenderer.close()
                parcelFileDescriptor.close()

                _miniaturas.value = bitmaps

                withContext(Dispatchers.Main) {
                    _estado.value = DividirPdfEstado.Listo(totalPaginas, nombre, tamano)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _estado.value = DividirPdfEstado.Error("Error al cargar PDF: ${e.message}")
                }
            }
        }
    }

    /**
     * Divide el PDF por un rango de páginas.
     * @param desde Página inicial (1-based).
     * @param hasta Página final (1-based).
     * @param nombreBase Nombre base para el archivo de salida.
     */
    fun dividirPorRango(desde: Int, hasta: Int, nombreBase: String) {
        val path = pdfPathTemporal
        if (path == null) {
            _estado.value = DividirPdfEstado.Error("No hay PDF cargado")
            return
        }

        viewModelScope.launch {
            _estado.value = DividirPdfEstado.Procesando(0f, "Dividiendo PDF...")

            val resultado = repository.dividirPdf(
                pdfPath = path,
                rangoDesde = desde,
                rangoHasta = hasta,
                nombreBase = nombreBase.ifBlank { "documento_dividido" },
                onProgreso = { progreso ->
                    _estado.value = DividirPdfEstado.Procesando(progreso, "Procesando...")
                }
            )

            resultado.fold(
                onSuccess = { ruta ->
                    withContext(Dispatchers.Main) {
                        _estado.value = DividirPdfEstado.Exito(listOf(ruta))
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _estado.value = DividirPdfEstado.Error(error.message ?: "Error al dividir PDF")
                    }
                }
            )
        }
    }

    /**
     * Divide el PDF en partes iguales.
     * @param nPartes Número de partes.
     * @param nombreBase Nombre base para los archivos de salida.
     */
    fun dividirEnPartes(nPartes: Int, nombreBase: String) {
        val path = pdfPathTemporal
        if (path == null) {
            _estado.value = DividirPdfEstado.Error("No hay PDF cargado")
            return
        }

        viewModelScope.launch {
            _estado.value = DividirPdfEstado.Procesando(0f, "Dividiendo PDF...")

            val resultado = repository.dividirEnPartes(
                pdfPath = path,
                nPartes = nPartes,
                nombreBase = nombreBase.ifBlank { "documento_dividido" },
                onProgreso = { progreso ->
                    _estado.value = DividirPdfEstado.Procesando(progreso, "Procesando...")
                }
            )

            resultado.fold(
                onSuccess = { archivos ->
                    withContext(Dispatchers.Main) {
                        _estado.value = DividirPdfEstado.Exito(archivos)
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _estado.value = DividirPdfEstado.Error(error.message ?: "Error al dividir PDF")
                    }
                }
            )
        }
    }

    /**
     * Restablece el estado a Idle.
     */
    fun resetearEstado() {
        _estado.value = if (_totalPaginas.value > 0) {
            DividirPdfEstado.Listo(_totalPaginas.value, _nombreArchivo.value, 0)
        } else {
            DividirPdfEstado.Idle
        }
    }

    /**
     * Cierra el PDF y limpia recursos.
     */
    fun cerrarPdf() {
        pdfPathTemporal?.let { File(it).delete() }
        pdfPathTemporal = null
        _miniaturas.value.forEach { it.recycle() }
        _miniaturas.value = emptyList()
        _pdfUri.value = null
        _totalPaginas.value = 0
        _nombreArchivo.value = ""
        _estado.value = DividirPdfEstado.Idle
    }

    override fun onCleared() {
        super.onCleared()
        cerrarPdf()
    }
}