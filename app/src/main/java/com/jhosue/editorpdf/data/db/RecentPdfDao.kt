package com.jhosue.editorpdf.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder a los PDFs recientes.
 */
@Dao
interface RecentPdfDao {

    /**
     * Obtiene todos los PDFs recientes ordenados por fecha de apertura descendente.
     * @return Flow con la lista de RecentPdfEntity.
     */
    @Query("SELECT * FROM recent_pdfs ORDER BY fechaApertura DESC LIMIT 20")
    fun obtenerTodos(): Flow<List<RecentPdfEntity>>

    /**
     * Guarda o actualiza un PDF reciente.
     * Si ya existe (por uriString), actualiza la fecha de apertura.
     * @param recentPdf Entidad a guardar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(recentPdf: RecentPdfEntity)

    /**
     * Elimina un PDF reciente por su URI.
     * @param uriString URI del PDF a eliminar.
     */
    @Query("DELETE FROM recent_pdfs WHERE uriString = :uriString")
    suspend fun eliminar(uriString: String)

    /**
     * Limpia todos los PDFs recientes.
     */
    @Query("DELETE FROM recent_pdfs")
    suspend fun limpiarTodo()

    /**
     * Obtiene un PDF reciente por su URI.
     * @param uriString URI del PDF a buscar.
     * @return RecentPdfEntity o null si no existe.
     */
    @Query("SELECT * FROM recent_pdfs WHERE uriString = :uriString LIMIT 1")
    suspend fun obtenerPorUri(uriString: String): RecentPdfEntity?
}