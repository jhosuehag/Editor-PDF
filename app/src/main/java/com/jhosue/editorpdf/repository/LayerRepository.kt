package com.jhosue.editorpdf.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jhosue.editorpdf.data.db.LayerDao
import com.jhosue.editorpdf.data.db.LayerEntity
import com.jhosue.editorpdf.data.db.PdfFixDatabase
import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.ContentEdit
import com.jhosue.editorpdf.data.models.LayerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar las capas de anotaciones y ediciones.
 * Utiliza Room para persistencia y Gson para serialización JSON.
 */
class LayerRepository(context: Context) {

    private val layerDao: LayerDao = PdfFixDatabase.getInstance(context).layerDao()
    private val gson: Gson = GsonBuilder().create()

    /**
     * Guarda una capa de página en la base de datos.
     * Serializa las anotaciones y ediciones a JSON.
     * @param layer Datos de la capa a guardar.
     */
    suspend fun guardarLayer(layer: LayerData) = withContext(Dispatchers.IO) {
        val anotacionesJson = gson.toJson(layer.anotaciones)
        val edicionesJson = gson.toJson(layer.ediciones)

        val entity = LayerEntity(
            pageIndex = layer.pageIndex,
            anotacionesJson = anotacionesJson,
            edicionesJson = edicionesJson
        )
        layerDao.guardar(entity)
    }

    /**
     * Obtiene una capa de página desde la base de datos.
     * Deserializa el JSON a listas de objetos.
     * @param pageIndex Índice de la página.
     * @return LayerData o null si no existe.
     */
    suspend fun obtenerLayer(pageIndex: Int): LayerData? = withContext(Dispatchers.IO) {
        val entity = layerDao.obtener(pageIndex) ?: return@withContext null

        val anotacionesType = object : TypeToken<List<AnnotationElement>>() {}.type
        val edicionesType = object : TypeToken<List<ContentEdit>>() {}.type

        val anotaciones: List<AnnotationElement> = gson.fromJson(
            entity.anotacionesJson,
            anotacionesType
        ) ?: emptyList()

        val ediciones: List<ContentEdit> = gson.fromJson(
            entity.edicionesJson,
            edicionesType
        ) ?: emptyList()

        LayerData(
            pageIndex = entity.pageIndex,
            anotaciones = anotaciones,
            ediciones = ediciones
        )
    }

    /**
     * Elimina la capa de una página específica.
     * @param pageIndex Índice de la página.
     */
    suspend fun limpiarLayer(pageIndex: Int) = withContext(Dispatchers.IO) {
        layerDao.limpiar(pageIndex)
    }

    /**
     * Elimina todas las capas guardadas.
     */
    suspend fun limpiarTodo() = withContext(Dispatchers.IO) {
        layerDao.limpiarTodo()
    }

    /**
     * Obtiene todas las capas guardadas.
     * @return Mapa de índice de página a LayerData.
     */
    suspend fun obtenerTodasLasCapas(): Map<Int, com.jhosue.editorpdf.data.models.LayerData> = withContext(Dispatchers.IO) {
        val entidades = layerDao.obtenerTodas()
        val resultado = mutableMapOf<Int, com.jhosue.editorpdf.data.models.LayerData>()
        
        val anotacionesType = object : com.google.gson.reflect.TypeToken<List<AnnotationElement>>() {}.type
        val edicionesType = object : com.google.gson.reflect.TypeToken<List<ContentEdit>>() {}.type
        
        for (entity in entidades) {
            val anotaciones: List<AnnotationElement> = gson.fromJson(entity.anotacionesJson, anotacionesType) ?: emptyList()
            val ediciones: List<ContentEdit> = gson.fromJson(entity.edicionesJson, edicionesType) ?: emptyList()
            
            resultado[entity.pageIndex] = com.jhosue.editorpdf.data.models.LayerData(
                pageIndex = entity.pageIndex,
                anotaciones = anotaciones,
                ediciones = ediciones
            )
        }
        
        resultado
    }
}