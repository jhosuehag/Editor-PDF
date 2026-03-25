package com.jhosue.editorpdf.utils

import android.util.Log
import com.jhosue.editorpdf.utils.commands.EditCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado del sistema Undo/Redo.
 * Mantiene un historial de comandos ejecutados y permite deshacer/rehacer.
 */
class UndoRedoManager {
    
    companion object {
        private const val TAG = "UndoRedoManager"
        /** Límite máximo de comandos en el historial */
        private const val LIMITE = 50
    }
    
    /** Cola de comandos ejecutados (historial de undo) */
    private val ejecutados = ArrayDeque<EditCommand>()
    
    /** Cola de comandos deshechos (historial de redo) */
    private val deshechos = ArrayDeque<EditCommand>()
    
    /** Indica si hay comandos para deshacer */
    private val _puedeDeshacer = MutableStateFlow(false)
    val puedeDeshacer: StateFlow<Boolean> = _puedeDeshacer.asStateFlow()
    
    /** Indica si hay comandos para rehacer */
    private val _puedeRehacer = MutableStateFlow(false)
    val puedeRehacer: StateFlow<Boolean> = _puedeRehacer.asStateFlow()
    
    /**
     * Ejecuta un comando y lo agrega al historial.
     * Limpia el historial de redo al ejecutar un nuevo comando.
     * @param comando Comando a ejecutar.
     */
    suspend fun ejecutar(comando: EditCommand) {
        try {
            comando.execute()
            ejecutados.addLast(comando)
            
            // Limitar el tamaño del historial
            if (ejecutados.size > LIMITE) {
                ejecutados.removeFirst()
            }
            
            // Limpiar historial de redo cuando se ejecuta un nuevo comando
            deshechos.clear()
            
            actualizarEstados()
            Log.d(TAG, "Comando ejecutado: ${comando.descripcion}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al ejecutar comando: ${comando.descripcion}", e)
        }
    }
    
    /**
     * Deshace el último comando ejecutado.
     */
    suspend fun deshacer() {
        if (ejecutados.isEmpty()) {
            Log.d(TAG, "No hay comandos para deshacer")
            return
        }
        
        val comando = ejecutados.removeLast()
        try {
            comando.undo()
            deshechos.addLast(comando)
            actualizarEstados()
            Log.d(TAG, "Comando deshecho: ${comando.descripcion}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al deshacer comando: ${comando.descripcion}", e)
            // Si falla el undo, intentamos agregar el comando de vuelta
            ejecutados.addLast(comando)
        }
    }
    
    /**
     * Rehace el último comando deshecho.
     */
    suspend fun rehacer() {
        if (deshechos.isEmpty()) {
            Log.d(TAG, "No hay comandos para rehacer")
            return
        }
        
        val comando = deshechos.removeLast()
        try {
            comando.execute()
            ejecutados.addLast(comando)
            actualizarEstados()
            Log.d(TAG, "Comando rehecho: ${comando.descripcion}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al rehacer comando: ${comando.descripcion}", e)
            // Si falla el execute, intentamos agregar el comando de vuelta
            deshechos.addLast(comando)
        }
    }
    
    /**
     * Limpia todo el historial de comandos.
     */
    fun limpiar() {
        ejecutados.clear()
        deshechos.clear()
        actualizarEstados()
        Log.d(TAG, "Historial de undo/redo limpiado")
    }
    
    /**
     * Obtiene el número de comandos en el historial de undo.
     */
    fun getCantidadUndo(): Int = ejecutados.size
    
    /**
     * Obtiene el número de comandos en el historial de redo.
     */
    fun getCantidadRedo(): Int = deshechos.size
    
    /**
     * Actualiza los estados de puedeDeshacer y puedeRehacer.
     */
    private fun actualizarEstados() {
        _puedeDeshacer.value = ejecutados.isNotEmpty()
        _puedeRehacer.value = deshechos.isNotEmpty()
    }
}