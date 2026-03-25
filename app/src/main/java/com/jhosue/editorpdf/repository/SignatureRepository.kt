package com.jhosue.editorpdf.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jhosue.editorpdf.data.db.PdfFixDatabase
import com.jhosue.editorpdf.data.db.SignatureEntity
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayOutputStream

/**
 * Repository para gestionar las firmas digitales.
 * Se encarga de la conversión de Bitmap a bytes y la persistencia en Room.
 */
class SignatureRepository(context: Context) {

    private val database = PdfFixDatabase.getInstance(context)
    private val signatureDao = database.signatureDao()

    /**
     * Guarda una firma en la base de datos.
     * @param bitmap Bitmap de la firma a guardar.
     * @param nombre Nombre para identificar la firma.
     */
    suspend fun guardarFirma(bitmap: Bitmap, nombre: String) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()

        val firma = SignatureEntity(
            nombre = nombre,
            fechaCreacion = System.currentTimeMillis(),
            bitmapBytes = bytes
        )
        signatureDao.guardar(firma)
    }

    /**
     * Obtiene todas las firmas guardadas como Flow.
     * @return Flow con la lista de firmas ordenadas por fecha.
     */
    fun obtenerFirmas(): Flow<List<SignatureEntity>> {
        return signatureDao.obtenerTodas()
    }

    /**
     * Elimina una firma por su ID.
     * @param id ID de la firma a eliminar.
     */
    suspend fun eliminarFirma(id: Int) {
        signatureDao.eliminar(id)
    }

    /**
     * Obtiene una firma por su ID y la convierte a Bitmap.
     * @param id ID de la firma.
     * @return Bitmap de la firma o null si no existe.
     */
    suspend fun obtenerFirmaBitmap(id: Int): Bitmap? {
        val firma = signatureDao.obtenerPorId(id)
        return firma?.let {
            BitmapFactory.decodeByteArray(it.bitmapBytes, 0, it.bitmapBytes.size)
        }
    }

    /**
     * Obtiene una entidad de firma por su ID.
     * @param id ID de la firma.
     * @return SignatureEntity o null si no existe.
     */
    suspend fun obtenerFirmaPorId(id: Int): SignatureEntity? {
        return signatureDao.obtenerPorId(id)
    }
}
