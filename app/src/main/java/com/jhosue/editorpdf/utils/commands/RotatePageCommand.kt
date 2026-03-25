package com.jhosue.editorpdf.utils.commands

import android.util.Log
import com.jhosue.editorpdf.repository.PageRepository

/**
 * Comando para rotar una página del PDF.
 * @param pageIndex Índice de la página a rotar (0-based).
 * @param grados Grados de rotación (90, 180, 270).
 * @param pageRepository Repositorio de páginas.
 * @param pdfPath Ruta del archivo PDF.
 */
class RotatePageCommand(
    private val pageIndex: Int,
    private val grados: Int,
    private val pageRepository: PageRepository,
    private val pdfPath: String
) : EditCommand {
    
    /** Rotación acumulada para poder revertir correctamente */
    private var rotacionAcumulada = grados
    
    override val descripcion: String = "Rotar página ${pageIndex + 1} (${grados}°)"

    /**
     * Ejecuta el comando: rota la página en la dirección especificada.
     */
    override suspend fun execute() {
        val exito = pageRepository.rotarPagina(pdfPath, pageIndex, grados)
        if (!exito) {
            Log.e("RotatePageCommand", "Error al rotar página $pageIndex")
        }
    }

    /**
     * Deshace el comando: rota la página en sentido contrario.
     */
    override suspend fun undo() {
        val gradosInverso = when (grados) {
            90 -> 270
            180 -> 180  // 180° es su propio inverso
            270 -> 90
            else -> grados
        }
        
        val exito = pageRepository.rotarPagina(pdfPath, pageIndex, gradosInverso)
        if (!exito) {
            Log.e("RotatePageCommand", "Error al deshacer rotación de página $pageIndex")
        }
    }
}