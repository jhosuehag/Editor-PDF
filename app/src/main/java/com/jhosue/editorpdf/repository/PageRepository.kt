package com.jhosue.editorpdf.repository

import android.util.Log
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repositorio para manejar operaciones de gestión de páginas de PDF usando iText7.
 * Proporciona métodos para eliminar, duplicar, insertar, rotar, reordenar y extraer páginas.
 */
class PageRepository {

    companion object {
        private const val TAG = "PageRepository"
    }

    /**
     * Crea una copia de seguridad del archivo PDF.
     * @param pdfPath Ruta del archivo PDF.
     * @return Ruta del archivo de backup o null si falla.
     */
    private fun crearBackup(pdfPath: String): String? {
        return try {
            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                Log.e(TAG, "Archivo PDF no encontrado: $pdfPath")
                return null
            }
            val backupPath = "$pdfPath.backup"
            archivo.copyTo(File(backupPath), overwrite = true)
            Log.d(TAG, "Backup creado: $backupPath")
            backupPath
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear backup", e)
            null
        }
    }

    /**
     * Restaura el PDF desde el backup.
     * @param pdfPath Ruta original del PDF.
     * @param backupPath Ruta del archivo de backup.
     * @return true si la restauración fue exitosa.
     */
    private fun restaurarBackup(pdfPath: String, backupPath: String): Boolean {
        return try {
            val backup = File(backupPath)
            if (backup.exists()) {
                backup.copyTo(File(pdfPath), overwrite = true)
                backup.delete()
                Log.d(TAG, "Backup restaurado exitosamente")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al restaurar backup", e)
            false
        }
    }

    /**
     * Elimina una página del PDF.
     * @param pdfPath Ruta del archivo PDF.
     * @param pageIndex Índice de la página a eliminar (0-based).
     * @return true si la operación fue exitosa.
     */
    suspend fun eliminarPagina(pdfPath: String, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        var backupPath: String? = null
        try {
            Log.d(TAG, "Eliminando página $pageIndex de $pdfPath")
            
            // Crear backup antes de modificar
            backupPath = crearBackup(pdfPath) ?: return@withContext false

            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val pdfWriter = PdfWriter(archivo)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // iText usa 1-based, eliminar la página
            val pageNumber = pageIndex + 1
            if (pageNumber < 1 || pageNumber > pdfDoc.numberOfPages) {
                Log.e(TAG, "Número de página inválido: $pageNumber")
                pdfDoc.close()
                return@withContext false
            }

            pdfDoc.removePage(pageNumber)
            pdfDoc.close()

            // Eliminar archivo de backup si todo salió bien
            backupPath?.let { File(it).delete() }

            Log.d(TAG, "Página eliminada exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar página", e)
            backupPath?.let { restaurarBackup(pdfPath, it) }
            false
        }
    }

    /**
     * Duplica una página en el PDF.
     * @param pdfPath Ruta del archivo PDF.
     * @param pageIndex Índice de la página a duplicar (0-based).
     * @return true si la operación fue exitosa.
     */
    suspend fun duplicarPagina(pdfPath: String, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        var backupPath: String? = null
        try {
            Log.d(TAG, "Duplicando página $pageIndex de $pdfPath")
            
            backupPath = crearBackup(pdfPath) ?: return@withContext false

            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val tempFile = File("$pdfPath.temp")
            val tempWriter = PdfWriter(tempFile)
            val tempDoc = PdfDocument(pdfReader, tempWriter)

            // Crear documento de salida
            val outputFile = File("$pdfPath.output")
            val outputWriter = PdfWriter(outputFile)
            val outputDoc = PdfDocument(outputWriter)

            // Copiar todas las páginas, insertando una copia después de la página seleccionada
            val pageNumber = pageIndex + 1
            for (i in 1..tempDoc.numberOfPages) {
                val page = tempDoc.getPage(i)
                outputDoc.addPage(page)
                // Después de la página especificada, insertar una copia
                if (i == pageNumber) {
                    outputDoc.addPage(tempDoc.getPage(i))
                }
            }

            tempDoc.close()
            pdfReader.close()
            outputDoc.close()

            // Reemplazar el original con el resultado
            archivo.delete()
            outputFile.copyTo(archivo, overwrite = true)
            outputFile.delete()

            Log.d(TAG, "Página duplicada exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al duplicar página", e)
            backupPath?.let { restaurarBackup(pdfPath, it) }
            false
        }
    }

    /**
     * Inserta una página en blanco en el PDF.
     * @param pdfPath Ruta del archivo PDF.
     * @param pageIndex Índice donde insertar la nueva página (0-based).
     * @return true si la operación fue exitosa.
     */
    suspend fun insertarPaginaBlanco(pdfPath: String, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        var backupPath: String? = null
        try {
            Log.d(TAG, "Insertando página en blanco en posición $pageIndex de $pdfPath")
            
            backupPath = crearBackup(pdfPath) ?: return@withContext false

            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val tempFile = File("$pdfPath.temp")
            val tempWriter = PdfWriter(tempFile)
            val tempDoc = PdfDocument(pdfReader, tempWriter)

            // Crear documento de salida
            val outputFile = File("$pdfPath.output")
            val outputWriter = PdfWriter(outputFile)
            val outputDoc = PdfDocument(outputWriter)

            // Copiar todas las páginas, insertando una página en blanco después de pageIndex
            for (i in 1..tempDoc.numberOfPages) {
                outputDoc.addPage(tempDoc.getPage(i))
                // Insertar página en blanco después de la página especificada
                if (i == pageIndex + 1) {
                    outputDoc.addNewPage(PageSize.A4)
                }
            }

            // Si se insertó al final (después de la última página)
            if (pageIndex >= tempDoc.numberOfPages) {
                outputDoc.addNewPage(PageSize.A4)
            }

            tempDoc.close()
            pdfReader.close()
            outputDoc.close()

            // Reemplazar el original con el resultado
            archivo.delete()
            outputFile.copyTo(archivo, overwrite = true)
            outputFile.delete()

            Log.d(TAG, "Página en blanco insertada exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al insertar página en blanco", e)
            backupPath?.let { restaurarBackup(pdfPath, it) }
            false
        }
    }

    /**
     * Rota una página del PDF.
     * @param pdfPath Ruta del archivo PDF.
     * @param pageIndex Índice de la página a rotar (0-based).
     * @param grados Grados de rotación (90, 180, 270).
     * @return true si la operación fue exitosa.
     */
    suspend fun rotarPagina(pdfPath: String, pageIndex: Int, grados: Int): Boolean = withContext(Dispatchers.IO) {
        var backupPath: String? = null
        try {
            Log.d(TAG, "Rotando página $pageIndex en $grados grados de $pdfPath")
            
            backupPath = crearBackup(pdfPath) ?: return@withContext false

            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val pdfWriter = PdfWriter(archivo)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // iText usa 1-based
            val pageNumber = pageIndex + 1
            if (pageNumber < 1 || pageNumber > pdfDoc.numberOfPages) {
                Log.e(TAG, "Número de página inválido: $pageNumber")
                pdfDoc.close()
                return@withContext false
            }

            val page = pdfDoc.getPage(pageNumber)
            val rotacionActual = page.rotation
            val nuevaRotacion = (rotacionActual + grados) % 360
            page.setRotation(nuevaRotacion)

            pdfDoc.close()

            // Eliminar backup si todo salió bien
            backupPath?.let { File(it).delete() }

            Log.d(TAG, "Página rotada exitosamente a $nuevaRotacion grados")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al rotar página", e)
            backupPath?.let { restaurarBackup(pdfPath, it) }
            false
        }
    }

    /**
     * Reordena las páginas del PDF según el nuevo orden especificado.
     * @param pdfPath Ruta del archivo PDF.
     * @param nuevoOrden Lista de índices (0-based) con el nuevo orden de páginas.
     * @return true si la operación fue exitosa.
     */
    suspend fun reordenarPaginas(pdfPath: String, nuevoOrden: List<Int>): Boolean = withContext(Dispatchers.IO) {
        var backupPath: String? = null
        try {
            Log.d(TAG, "Reordenando páginas de $pdfPath")
            Log.d(TAG, "Nuevo orden: $nuevoOrden")
            
            if (nuevoOrden.isEmpty() || nuevoOrden.contains(-1) || nuevoOrden.distinct().size != nuevoOrden.size) {
                Log.e(TAG, "Orden inválido: $nuevoOrden")
                return@withContext false
            }

            backupPath = crearBackup(pdfPath) ?: return@withContext false

            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val tempFile = File("$pdfPath.temp")
            val tempWriter = PdfWriter(tempFile)
            val tempDoc = PdfDocument(pdfReader, tempWriter)

            // Crear documento de salida
            val outputFile = File("$pdfPath.output")
            val outputWriter = PdfWriter(outputFile)
            val outputDoc = PdfDocument(outputWriter)

            // Copiar páginas en el nuevo orden
            for (pageIndex in nuevoOrden) {
                // iText usa 1-based
                val pageNumber = pageIndex + 1
                if (pageNumber in 1..tempDoc.numberOfPages) {
                    outputDoc.addPage(tempDoc.getPage(pageNumber))
                }
            }

            tempDoc.close()
            pdfReader.close()
            outputDoc.close()

            // Verificar que se copiaron todas las páginas
            val resultReader = PdfReader(archivo)
            val resultDoc = PdfDocument(resultReader)
            val pagesCopied = resultDoc.numberOfPages
            resultDoc.close()

            if (pagesCopied != nuevoOrden.size) {
                Log.e(TAG, "Error: número de páginas copiadas ($pagesCopied) no coincide con esperado (${nuevoOrden.size})")
                archivo.delete()
                backupPath?.let { restaurarBackup(pdfPath, it) }
                return@withContext false
            }

            // Reemplazar el original con el resultado
            archivo.delete()
            outputFile.copyTo(archivo, overwrite = true)
            outputFile.delete()

            Log.d(TAG, "Páginas reordenadas exitosamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al reordenar páginas", e)
            backupPath?.let { restaurarBackup(pdfPath, it) }
            false
        }
    }

    /**
     * Extrae una página específica del PDF como un nuevo archivo.
     * @param pdfPath Ruta del archivo PDF origen.
     * @param pageIndex Índice de la página a extraer (0-based).
     * @param destinoPath Ruta de destino para el nuevo PDF.
     * @return true si la operación fue exitosa.
     */
    suspend fun extraerPagina(pdfPath: String, pageIndex: Int, destinoPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extrayendo página $pageIndex de $pdfPath a $destinoPath")

            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                Log.e(TAG, "Archivo PDF no encontrado: $pdfPath")
                return@withContext false
            }

            val pdfReader = PdfReader(archivo)
            val sourceDoc = PdfDocument(pdfReader)

            // iText usa 1-based
            val pageNumber = pageIndex + 1
            if (pageNumber < 1 || pageNumber > sourceDoc.numberOfPages) {
                Log.e(TAG, "Número de página inválido: $pageNumber")
                sourceDoc.close()
                return@withContext false
            }

            // Crear nuevo documento con solo la página seleccionada
            val destFile = File(destinoPath)
            val destWriter = PdfWriter(destFile)
            val destDoc = PdfDocument(destWriter)

            destDoc.addPage(sourceDoc.getPage(pageNumber))

            destDoc.close()
            sourceDoc.close()

            Log.d(TAG, "Página extraída exitosamente a $destinoPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer página", e)
            false
        }
    }
}