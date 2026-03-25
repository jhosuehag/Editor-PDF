package com.jhosue.editorpdf.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * DAO para acceder a las capas de páginas en la base de datos.
 */
@Dao
interface LayerDao {

    /**
     * Inserta o actualiza una capa en la base de datos.
     * @param layer Entidad de capa a guardar.
     */
    @Upsert
    suspend fun guardar(layer: LayerEntity)

    /**
     * Obtiene la capa de una página específica.
     * @param index Índice de la página.
     * @return LayerEntity o null si no existe.
     */
    @Query("SELECT * FROM layers WHERE pageIndex = :index")
    suspend fun obtener(index: Int): LayerEntity?

    /**
     * Elimina la capa de una página específica.
     * @param index Índice de la página.
     */
    @Query("DELETE FROM layers WHERE pageIndex = :index")
    suspend fun limpiar(index: Int)

    /**
     * Elimina todas las capas guardadas.
     */
    @Query("DELETE FROM layers")
    suspend fun limpiarTodo()

    /**
     * Obtiene todas las capas guardadas.
     * @return Lista de todas las entidades de capa.
     */
    @Query("SELECT * FROM layers")
    suspend fun obtenerTodas(): List<LayerEntity>
}