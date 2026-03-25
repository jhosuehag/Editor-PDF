package com.jhosue.editorpdf.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jhosue.editorpdf.data.db.SignatureEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceder a las firmas digitales en la base de datos.
 */
@Dao
interface SignatureDao {

    /**
     * Inserta una nueva firma en la base de datos.
     * @param firma Entidad de firma a guardar.
     * @return ID de la firma insertada.
     */
    @Insert
    suspend fun guardar(firma: SignatureEntity): Long

    /**
     * Obtiene todas las firmas ordenadas por fecha de creación descendente.
     * @return Flow con la lista de firmas.
     */
    @Query("SELECT * FROM signatures ORDER BY fechaCreacion DESC")
    fun obtenerTodas(): Flow<List<SignatureEntity>>

    /**
     * Elimina una firma por su ID.
     * @param id ID de la firma a eliminar.
     */
    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun eliminar(id: Int)

    /**
     * Obtiene una firma específica por su ID.
     * @param id ID de la firma.
     * @return SignatureEntity o null si no existe.
     */
    @Query("SELECT * FROM signatures WHERE id = :id")
    suspend fun obtenerPorId(id: Int): SignatureEntity?
}
