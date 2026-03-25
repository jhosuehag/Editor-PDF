package com.jhosue.editorpdf.utils.commands

import android.util.Log
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.jhosue.editorpdf.repository.PageRepository
import java.io.File

/**
 * Comando para eliminar una página del PDF.
 * Permite deshacer la eliminación.
 * @param pageIndex Índice de la página a eliminar (0-based).
 * @param pageRepository Repositorio de páginas.
 * @param pdfPath Ruta del archivo PDF.
 * @param totalPaginasBefore Número total de páginas antes de eliminar.
 */
class DeletePageCommand(
    private val pageIndex: Int,
    private val pageRepository: PageRepository,
    private val pdfPath: String,
    private val totalPaginasBefore: Int
) : EditCommand {
    
    override val descripcion: String = "Eliminar página ${pageIndex + 1}"
    
    /** Contenido de la página eliminada para posible restauración */
    private var paginaContenido: ByteArray? = null

    /**
     * Ejecuta el comando: elimina la página del PDF.
     */
    override suspend fun execute() {
        // Guardar contenido de la página antes de eliminar
        guardarContenidoPagina()
        
        // Realizar eliminación
        val exito = pageRepository.eliminarPagina(pdfPath, pageIndex)
        if (!exito) {
            Log.e("DeletePageCommand", "Error al eliminar página $pageIndex")
        }
    }

    /**
     * Deshace el comando: restaura la página eliminada.
     * Para restaurar, insertamos una página en blanco en la posición
     * y luego el contenido guardado se pierde - por eso guardamos backup completo.
     */
    override suspend fun undo() {
        // Restaurar desde el backup creado por PageRepository
        val backupFile = File("$pdfPath.backup")
        if (backupFile.exists()) {
            val archivoActual = File(pdfPath)
            // Copiar el backup sobre el archivo actual
            backupFile.copyTo(archivoActual, overwrite = true)
            backupFile.delete()
            Log.d("DeletePageCommand", "Página restaurada desde backup")
        } else {
            Log.e("DeletePageCommand", "No se encontró backup para restaurar")
        }
    }

    /**
     * Guarda el contenido de la página antes de eliminarla.
     */
    private fun guardarContenidoPagina() {
        try {
            val archivo = File(pdfPath)
            val pdfReader = PdfReader(archivo)
            val srcDoc = PdfDocument(pdfReader)
            
            // Crear documento temporal con solo esta página
            val tempFile = File("$pdfPath.page$pageIndex.temp")
            val tempWriter = PdfWriter(tempFile)
            val tempDoc = PdfDocument(tempWriter)
            
            tempDoc.addPage(srcDoc.getPage(pageIndex + 1))
            
            tempDoc.close()
            srcDoc.close()
            
            paginaContenido = tempFile.readBytes()
            tempFile.delete()
            
        } catch (e: Exception) {
            Log.e("DeletePageCommand", "Error al guardar contenido de página", e)
        }
    }
}