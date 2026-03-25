package com.jhosue.editorpdf.repository

import android.content.Context
import android.net.Uri
import com.jhosue.editorpdf.data.db.PdfFixDatabase
import com.jhosue.editorpdf.data.db.RecentPdfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar los PDFs recientes abiertos.
 * Proporciona métodos para guardar, obtener y eliminar historial de PDFs.
 */
class RecentPdfRepository(context: Context) {

    private val recentPdfDao = PdfFixDatabase.getInstance(context).recentPdfDao()

    /**
     * Obtiene todos los PDFs recientes como Flow.
     * @return Flow con lista de RecentPdfEntity ordenados por fecha.
     */
    fun obtenerRecientes(): Flow<List<RecentPdfEntity>> {
        return recentPdfDao.obtenerTodos()
    }

    /**
     * Guarda o actualiza un PDF en el historial.
     * Si el PDF ya existe (por URI), actualiza la fecha de apertura.
     * @param uri URI del PDF.
     * @param nombre Nombre del archivo.
     * @param tamanoBytes Tamaño en bytes (opcional).
     */
    suspend fun guardarPdfReciente(uri: Uri, nombre: String, tamanoBytes: Long = 0) = withContext(Dispatchers.IO) {
        val entity = RecentPdfEntity(
            uriString = uri.toString(),
            nombre = nombre,
            fechaApertura = System.currentTimeMillis(),
            tamanoBytes = tamanoBytes
        )
        recentPdfDao.guardar(entity)
    }

    /**
     * Elimina un PDF del historial por su URI.
     * @param uri URI del PDF a eliminar.
     */
    suspend fun eliminarPdfReciente(uri: Uri) = withContext(Dispatchers.IO) {
        recentPdfDao.eliminar(uri.toString())
    }

    /**
     * Limpia todo el historial de PDFs recientes.
     */
    suspend fun limpiarHistorial() = withContext(Dispatchers.IO) {
        recentPdfDao.limpiarTodo()
    }

    /**
     * Obtiene un PDF reciente por su URI.
     * @param uri URI del PDF a buscar.
     * @return RecentPdfEntity o null si no existe.
     */
    suspend fun obtenerPorUri(uri: Uri): RecentPdfEntity? = withContext(Dispatchers.IO) {
        recentPdfDao.obtenerPorUri(uri.toString())
    }
}