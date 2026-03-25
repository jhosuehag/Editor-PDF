package com.jhosue.editorpdf.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jhosue.editorpdf.repository.DocumentToolsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de operación para convertir imágenes a PDF.
 */
sealed class ImagenesPdfEstado {
    /** Estado inicial, sin operación en curso. */
    data object Idle : ImagenesPdfEstado()
    /** Operación en curso con progreso (0.0 a 1.0). */
    data class Procesando(val progreso: Float, val mensaje: String) : ImagenesPdfEstado()
    /** Operación exitosa con la ruta del archivo creado. */
    data class Exito(val rutaArchivo: String) : ImagenesPdfEstado()
    /** Operación fallida con mensaje de error. */
    data class Error(val mensaje: String) : ImagenesPdfEstado()
}

/**
 * ViewModel para la pantalla de convertir imágenes a PDF.
 * Maneja la selección de imágenes y la operación de conversión.
 */
class ImagenesPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentToolsRepository()

    /** Estado actual de la operación. */
    private val _estado = MutableStateFlow<ImagenesPdfEstado>(ImagenesPdfEstado.Idle)
    val estado: StateFlow<ImagenesPdfEstado> = _estado.asStateFlow()

    /** Lista de URIs de las imágenes seleccionadas. */
    private val _imagenesUri = MutableStateFlow<List<Uri>>(emptyList())
    val imagenesUri: StateFlow<List<Uri>> = _imagenesUri.asStateFlow()

    /** Lista de bitmaps de las imágenes para mostrar. */
    private val _imagenesBitmap = MutableStateFlow<List<Bitmap>>(emptyList())
    val imagenesBitmap: StateFlow<List<Bitmap>> = _imagenesBitmap.asStateFlow()

    /** Orientación seleccionada: "VERTICAL" u "HORIZONTAL". */
    private val _orientacion = MutableStateFlow("VERTICAL")
    val orientacion: StateFlow<String> = _orientacion.asStateFlow()

    /** Margen seleccionado en puntos: 0, 10, 20. */
    private val _margen = MutableStateFlow(20f)
    val margen: StateFlow<Float> = _margen.asStateFlow()

    /**
     * Agrega imágenes a la lista.
     * @param uris Lista de URIs de las imágenes.
     */
    fun agregarImagenes(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val urisActuales = _imagenesUri.value.toMutableList()
            val bitmapsActuales = _imagenesBitmap.value.toMutableList()

            uris.forEach { uri ->
                if (!urisActuales.contains(uri)) {
                    urisActuales.add(uri)

                    // Cargar bitmap para previsualización
                    val bitmap = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    bitmap?.let { bitmapsActuales.add(it) }
                }
            }

            _imagenesUri.value = urisActuales
            _imagenesBitmap.value = bitmapsActuales
        }
    }

    /**
     * Elimina una imagen por índice.
     * @param indice Índice de la imagen a eliminar.
     */
    fun eliminarImagen(indice: Int) {
        val urisActuales = _imagenesUri.value.toMutableList()
        val bitmapsActuales = _imagenesBitmap.value.toMutableList()

        if (indice in urisActuales.indices) {
            // Reciclar el bitmap
            bitmapsActuales.getOrNull(indice)?.recycle()
            urisActuales.removeAt(indice)
            bitmapsActuales.removeAt(indice)

            _imagenesUri.value = urisActuales
            _imagenesBitmap.value = bitmapsActuales
        }
    }

    /**
     * Establece la orientación de las páginas.
     * @param orientacion "VERTICAL" u "HORIZONTAL".
     */
    fun setOrientacion(orientacion: String) {
        _orientacion.value = orientacion
    }

    /**
     * Establece el margen de las páginas.
     * @param margen Margen en puntos (0, 10, 20).
     */
    fun setMargen(margen: Float) {
        _margen.value = margen
    }

    /**
     * Ejecuta la conversión de imágenes a PDF.
     * @param nombreDestino Nombre del archivo PDF resultante.
     */
    fun convertirAPdf(nombreDestino: String) {
        val uris = _imagenesUri.value
        if (uris.isEmpty()) {
            _estado.value = ImagenesPdfEstado.Error("No hay imágenes para convertir")
            return
        }

        viewModelScope.launch {
            _estado.value = ImagenesPdfEstado.Procesando(0f, "Preparando imágenes...")

            val resultado = repository.imagenesToPdf(
                uris = uris,
                context = getApplication(),
                orientacion = _orientacion.value,
                margen = _margen.value,
                nombreDestino = nombreDestino.ifBlank { "documento_imagenes" },
                onProgreso = { progreso ->
                    val mensaje = when {
                        progreso < 0.3f -> "Procesando imágenes..."
                        progreso < 0.7f -> "Creando páginas..."
                        else -> "Finalizando PDF..."
                    }
                    _estado.value = ImagenesPdfEstado.Procesando(progreso, mensaje)
                }
            )

            resultado.fold(
                onSuccess = { ruta ->
                    withContext(Dispatchers.Main) {
                        _estado.value = ImagenesPdfEstado.Exito(ruta)
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _estado.value = ImagenesPdfEstado.Error(error.message ?: "Error desconocido al convertir")
                    }
                }
            )
        }
    }

    /**
     * Restablece el estado a Idle.
     */
    fun resetearEstado() {
        _estado.value = ImagenesPdfEstado.Idle
    }

    /**
     * Limpia todas las imágenes seleccionadas.
     */
    fun limpiarImagenes() {
        _imagenesBitmap.value.forEach { it.recycle() }
        _imagenesBitmap.value = emptyList()
        _imagenesUri.value = emptyList()
        _estado.value = ImagenesPdfEstado.Idle
    }

    override fun onCleared() {
        super.onCleared()
        _imagenesBitmap.value.forEach { it.recycle() }
    }
}