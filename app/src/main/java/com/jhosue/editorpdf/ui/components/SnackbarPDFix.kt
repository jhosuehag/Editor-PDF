package com.jhosue.editorpdf.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SnackbarTipo {
    EXITO, ERROR, INFO
}

/**
 * Componente personalizado de Snackbar para feedback de operaciones.
 */
@Composable
fun SnackbarPDFix(
    mensaje: String,
    tipo: SnackbarTipo,
    onDismissRequest: () -> Unit = {}
) {
    val backgroundColor = when (tipo) {
        SnackbarTipo.EXITO -> Color(0xFF16A34A)
        SnackbarTipo.ERROR -> MaterialTheme.colorScheme.error
        SnackbarTipo.INFO -> MaterialTheme.colorScheme.primary
    }

    val icon = when (tipo) {
        SnackbarTipo.EXITO -> Icons.Default.CheckCircle
        SnackbarTipo.ERROR -> Icons.Default.Error
        SnackbarTipo.INFO -> Icons.Default.Info
    }

    Snackbar(
        modifier = Modifier.padding(12.dp),
        containerColor = backgroundColor,
        contentColor = Color.White,
        dismissAction = {
            IconButton(onClick = onDismissRequest) {
                Icon(Icons.Default.Info, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
