package com.jhosue.editorpdf.data.models

/**
 * Estado del PDF en el editor.
 * Representa los posibles estados de carga y visualización.
 */
sealed class PdfState {
    /**
     * Estado inicial, no se ha abierto ningún PDF.
     */
    object Idle : PdfState()

    /**
     * Estado de carga mientras se abre o renderiza el PDF.
     */
    object Loading : PdfState()

    /**
     * Estado de éxito con información del PDF cargado.
     * @property totalPages Número total de páginas del documento.
     * @property fileName Nombre del archivo PDF.
     */
    data class Success(
        val totalPages: Int,
        val fileName: String
    ) : PdfState()

    /**
     * Estado de error cuando no se puede abrir o renderizar el PDF.
     * @property mensaje Descripción del error ocurrido.
     */
    data class Error(
        val mensaje: String
    ) : PdfState()
}