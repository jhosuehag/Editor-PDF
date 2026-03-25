package com.jhosue.editorpdf.viewmodel

import android.app.Application
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
 * Estado de operación para unir PDFs.
 */
sealed class UnirPdfEstado {
    /** Estado inicial, sin operación en curso. */
    data object Idle : UnirPdfEstado()
    /** Operación en curso con progreso (0.0 a 1.0). */
    data class Procesando(val progreso: Float, val mensaje: String) : UnirPdfEstado()
    /** Operación exitosa con la ruta del archivo creado. */
    data class Exito(val rutaArchivo: String) : UnirPdfEstado()
    /** Operación fallida con mensaje de error. */
    data class Error(val mensaje: String) : UnirPdfEstado()
}

/**
 * ViewModel para la pantalla de unir PDFs.
 * Maneja la selección de archivos y la operación de fusión.
 */
class UnirPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentToolsRepository()

    /** Estado actual de la operación de unir PDFs. */
    private val _estado = MutableStateFlow<UnirPdfEstado>(UnirPdfEstado.Idle)
    val estado: StateFlow<UnirPdfEstado> = _estado.asStateFlow()

    /** Lista de URIs de los archivos PDF seleccionados. */
    private val _archivosSeleccionados = MutableStateFlow<List<Uri>>(emptyList())
    val archivosSeleccionados: StateFlow<List<Uri>> = _archivosSeleccionados.asStateFlow()

    /** Nombres de archivos para mostrar (extraídos de URIs). */
    private val _nombresArchivos = MutableStateFlow<List<String>>(emptyList())
    val nombresArchivos: StateFlow<List<String>> = _nombresArchivos.asStateFlow()

    /**
     * Agrega archivos PDF a la lista de archivos a unir.
     * @param uris Lista de URIs de los archivos.
     */
    fun agregarArchivos(uris: List<Uri>) {
        val archivosActuales = _archivosSeleccionados.value.toMutableList()
        archivosActuales.addAll(uris)
        _archivosSeleccionados.value = archivosActuales
        
        // Extraer nombres de archivo
        val nombres = uris.map { uri ->
            getApplication<Application>().contentResolver.query(
                uri, null, null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment ?: "desconocido"
            } ?: uri.lastPathSegment ?: "desconocido"
        }
        val nombresActuales = _nombresArchivos.value.toMutableList()
        nombresActuales.addAll(nombres)
        _nombresArchivos.value = nombresActuales
    }

    /**
     * Elimina un archivo de la lista por índice.
     * @param indice Índice del archivo a eliminar.
     */
    fun eliminarArchivo(indice: Int) {
        val archivosActuales = _archivosSeleccionados.value.toMutableList()
        val nombresActuales = _nombresArchivos.value.toMutableList()
        if (indice in archivosActuales.indices) {
            archivosActuales.removeAt(indice)
            nombresActuales.removeAt(indice)
            _archivosSeleccionados.value = archivosActuales
            _nombresArchivos.value = nombresActuales
        }
    }

    /**
     * Reordena un archivo dentro de la lista.
     * @param de Índice actual del archivo.
     * @param a Nuevo índice del archivo.
     */
    fun reordenarArchivo(de: Int, a: Int) {
        if (de == a) return
        val archivosActuales = _archivosSeleccionados.value.toMutableList()
        val nombresActuales = _nombresArchivos.value.toMutableList()
        val archivoMovido = archivosActuales.removeAt(de)
        val nombreMovido = nombresActuales.removeAt(de)
        archivosActuales.add(a, archivoMovido)
        nombresActuales.add(a, nombreMovido)
        _archivosSeleccionados.value = archivosActuales
        _nombresArchivos.value = nombresActuales
    }

    /**
     * Ejecuta la operación de unir los PDFs seleccionados.
     * @param nombreDestino Nombre del archivo PDF resultante.
     */
    fun unirPdfs(nombreDestino: String) {
        val archivos = _archivosSeleccionados.value
        if (archivos.size < 2) {
            _estado.value = UnirPdfEstado.Error("Se necesitan al menos 2 archivos para unir")
            return
        }

        viewModelScope.launch {
            _estado.value = UnirPdfEstado.Procesando(0f, "Preparando archivos...")

            val resultado = repository.unirPdfs(
                uris = archivos,
                context = getApplication(),
                nombreDestino = nombreDestino.ifBlank { "documento_unido" },
                onProgreso = { progreso ->
                    val mensaje = when {
                        progreso < 0.3f -> "Copiando archivos..."
                        progreso < 0.7f -> "Fusionando contenido..."
                        else -> "Finalizando..."
                    }
                    _estado.value = UnirPdfEstado.Procesando(progreso, mensaje)
                }
            )

            resultado.fold(
                onSuccess = { ruta ->
                    withContext(Dispatchers.Main) {
                        _estado.value = UnirPdfEstado.Exito(ruta)
                    }
                },
                onFailure = { error ->
                    withContext(Dispatchers.Main) {
                        _estado.value = UnirPdfEstado.Error(error.message ?: "Error desconocido al unir PDFs")
                    }
                }
            )
        }
    }

    /**
     * Restablece el estado a Idle.
     */
    fun resetearEstado() {
        _estado.value = UnirPdfEstado.Idle
    }

    /**
     * Limpia todos los archivos seleccionados.
     */
    fun limpiarArchivos() {
        _archivosSeleccionados.value = emptyList()
        _nombresArchivos.value = emptyList()
        _estado.value = UnirPdfEstado.Idle
    }
}