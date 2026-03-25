package com.jhosue.editorpdf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Callbacks para las operaciones de guardado.
 */
data class GuardarCallbacks(
    val onGuardar: () -> Unit,
    val onGuardarComo: (String) -> Unit
)

/**
 * BottomSheet para opciones de guardado del documento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetGuardar(
    onDismiss: () -> Unit,
    callbacks: GuardarCallbacks
) {
    var mostrarGuardarComo by remember { mutableStateOf(false) }
    var nombreTexto by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Barra indicadora superior
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(40.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            Text(
                text = "Guardar documento",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Opción: Guardar (Sobreescribir)
            ListItem(
                modifier = Modifier.clickable { 
                    callbacks.onGuardar()
                    onDismiss()
                },
                leadingContent = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text("Guardar", style = MaterialTheme.typography.bodyMedium) },
                supportingContent = { 
                    Text(
                        "Sobreescribir el archivo original", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Opción: Guardar como
            ListItem(
                modifier = Modifier.clickable { mostrarGuardarComo = !mostrarGuardarComo },
                leadingContent = { Icon(Icons.Default.SaveAs, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                headlineContent = { Text("Guardar como", style = MaterialTheme.typography.bodyMedium) },
                supportingContent = { 
                    Text(
                        "Crear una copia con nuevo nombre", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            AnimatedVisibility(visible = mostrarGuardarComo) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = nombreTexto,
                        onValueChange = { nombreTexto = it },
                        label = { Text("Nombre del archivo") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Text(".pdf", modifier = Modifier.padding(end = 8.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { 
                            if (nombreTexto.isNotBlank()) {
                                callbacks.onGuardarComo(nombreTexto.trim())
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = nombreTexto.isNotBlank()
                    ) {
                        Text("Guardar copia")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}