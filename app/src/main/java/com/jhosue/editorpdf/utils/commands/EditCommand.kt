package com.jhosue.editorpdf.utils.commands

/**
 * Interfaz base para comandos de edición en el editor de PDF.
 * Cada comando representa una acción que puede ser ejecutada y deshecha.
 */
interface EditCommand {
    /**
     * Ejecuta la acción del comando.
     */
    suspend fun execute()
    
    /**
     * Deshace la acción del comando.
     */
    suspend fun undo()
    
    /**
     * Descripción legible del comando para debugging.
     */
    val descripcion: String
}