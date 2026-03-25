package com.jhosue.editorpdf.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jhosue.editorpdf.data.db.SignatureEntity
import com.jhosue.editorpdf.repository.SignatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Firma digital.
 * Gestiona el estado de las firmas guardadas y las operaciones CRUD.
 */
class FirmaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SignatureRepository(application)

    /**
     * Lista de firmas guardadas en la base de datos.
     */
    private val _firmas = MutableStateFlow<List<SignatureEntity>>(emptyList())
    val firmas: StateFlow<List<SignatureEntity>> = _firmas.asStateFlow()

    /**
     * Bitmap de la firma seleccionada para insertar en el PDF.
     */
    private val _firmaSeleccionadaBitmap = MutableStateFlow<Bitmap?>(null)
    val firmaSeleccionadaBitmap: StateFlow<Bitmap?> = _firmaSeleccionadaBitmap.asStateFlow()

    /**
     * ID de la firma seleccionada para insertar.
     */
    private val _firmaSeleccionadaId = MutableStateFlow<Int?>(null)
    val firmaSeleccionadaId: StateFlow<Int?> = _firmaSeleccionadaId.asStateFlow()

    init {
        cargarFirmas()
    }

    /**
     * Carga todas las firmas desde el repository.
     */
    private fun cargarFirmas() {
        viewModelScope.launch {
            repository.obtenerFirmas().collect { lista ->
                _firmas.value = lista
            }
        }
    }

    /**
     * Guarda una nueva firma.
     * @param bitmap Bitmap de la firma dibujada.
     * @param nombre Nombre para identificar la firma.
     */
    fun guardarFirma(bitmap: Bitmap, nombre: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.guardarFirma(bitmap, nombre)
        }
    }

    /**
     * Elimina una firma por su ID.
     * @param id ID de la firma a eliminar.
     */
    fun eliminarFirma(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarFirma(id)
        }
    }

    /**
     * Selecciona una firma para insertar en el PDF.
     * @param id ID de la firma seleccionada.
     */
    fun seleccionarFirmaParaInsertar(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _firmaSeleccionadaBitmap.value = repository.obtenerFirmaBitmap(id)
            _firmaSeleccionadaId.value = id
        }
    }

    /**
     * Limpia la selección de firma.
     */
    fun limpiarSeleccionFirma() {
        _firmaSeleccionadaBitmap.value = null
        _firmaSeleccionadaId.value = null
    }

    /**
     * Obtiene el bitmap de una firma por su ID.
     * @param id ID de la firma.
     * @return Bitmap de la firma o null.
     */
    suspend fun obtenerBitmapFirma(id: Int): Bitmap? {
        return repository.obtenerFirmaBitmap(id)
    }
}
