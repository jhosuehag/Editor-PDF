package com.jhosue.editorpdf.utils.commands

import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.LayerData
import com.jhosue.editorpdf.repository.LayerRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Comando para eliminar una anotación del layer.
 * @param anotacion Anotación a eliminar.
 * @param layerRepository Repositorio de layers para persistencia.
 * @param layerData Flow del layer actual para actualización de UI.
 */
class RemoveAnnotationCommand(
    private val anotacion: AnnotationElement,
    private val layerRepository: LayerRepository,
    private val layerData: MutableStateFlow<LayerData>
) : EditCommand {
    
    override val descripcion: String = "Eliminar ${anotacion.javaClass.simpleName}"

    /**
     * Ejecuta el comando: elimina la anotación del layer.
     */
    override suspend fun execute() {
        val layerActual = layerData.value
        val layerModificado = layerActual.copy(
            anotaciones = layerActual.anotaciones.filter { it.id != anotacion.id }
        )
        layerData.value = layerModificado
        layerRepository.guardarLayer(layerModificado)
    }

    /**
     * Deshace el comando: restaura la anotación al layer.
     */
    override suspend fun undo() {
        val layerActual = layerData.value
        val layerRestaurado = layerActual.copy(
            pageIndex = anotacion.pageIndex,
            anotaciones = layerActual.anotaciones + anotacion
        )
        layerData.value = layerRestaurado
        layerRepository.guardarLayer(layerRestaurado)
    }
}