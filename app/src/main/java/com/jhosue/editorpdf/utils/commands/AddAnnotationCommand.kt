package com.jhosue.editorpdf.utils.commands

import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.LayerData
import com.jhosue.editorpdf.repository.LayerRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Comando para agregar una anotación al layer.
 * @param anotacion Anotación a agregar.
 * @param layerRepository Repositorio de layers para persistencia.
 * @param layerData Flow del layer actual para actualización de UI.
 */
class AddAnnotationCommand(
    private val anotacion: AnnotationElement,
    private val layerRepository: LayerRepository,
    private val layerData: MutableStateFlow<LayerData>
) : EditCommand {
    
    override val descripcion: String = "Agregar ${anotacion.javaClass.simpleName}"

    /**
     * Ejecuta el comando: agrega la anotación al layer y lo guarda.
     */
    override suspend fun execute() {
        val layerActual = layerData.value
        val nuevoLayer = layerActual.copy(
            pageIndex = anotacion.pageIndex,
            anotaciones = layerActual.anotaciones + anotacion
        )
        layerData.value = nuevoLayer
        layerRepository.guardarLayer(nuevoLayer)
    }

    /**
     * Deshace el comando: elimina la anotación del layer por su ID.
     */
    override suspend fun undo() {
        val layerActual = layerData.value
        val layerRestaurado = layerActual.copy(
            anotaciones = layerActual.anotaciones.filter { it.id != anotacion.id }
        )
        layerData.value = layerRestaurado
        layerRepository.guardarLayer(layerRestaurado)
    }
}