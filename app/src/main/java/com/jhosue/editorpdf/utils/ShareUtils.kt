package com.jhosue.editorpdf.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utilidades para compartir archivos PDF.
 */
object ShareUtils {

    /**
     * Comparte un archivo PDF usando el sistema de compartición de Android.
     * Utiliza FileProvider para compartir archivos de forma segura.
     * @param context Contexto de la aplicación.
     * @param uri URI del archivo PDF a compartir.
     */
    fun compartirPdf(context: Context, uri: Uri) {
        try {
            // Obtener la ruta del archivo desde la URI
            val path = uri.path ?: return
            
            // Crear File desde la ruta
            val file = File(path)
            if (!file.exists()) {
                return
            }

            // Obtener URI segura usando FileProvider
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            // Crear intent de envío
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Abrir selector de apps
            context.startActivity(
                Intent.createChooser(intent, "Compartir PDF")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}