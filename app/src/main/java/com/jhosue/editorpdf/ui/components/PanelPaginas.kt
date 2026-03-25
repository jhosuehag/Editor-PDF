package com.jhosue.editorpdf.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Panel de gestión de páginas para el editor de PDF.
 */
@Composable
fun PanelPaginas() {
    val paginasMock = (1..8).map { "Pág $it" }
    var paginaSeleccionada by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // FILA 1 — Barra de acciones globales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${paginasMock.size} páginas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { Toast.makeText(context, "Selecciona posición", Toast.LENGTH_SHORT).show() }) {
                Text("+ Insertar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { Toast.makeText(context, "Selecciona páginas a extraer", Toast.LENGTH_SHORT).show() }) {
                Text("Extraer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { Toast.makeText(context, "Mantén presionado para mover", Toast.LENGTH_SHORT).show() }) {
                Text("Reordenar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        // FILA 2 — Grilla de miniaturas
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(paginasMock) { index, titulo ->
                PageThumbnailItem(
                    index = index,
                    isSelected = paginaSeleccionada == index,
                    onClick = { paginaSeleccionada = index }
                )
            }
        }
    }
}

/**
 * Componente individual de miniatura de página con acciones
 */
@Composable
fun PageThumbnailItem(index: Int, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val numero = index + 1

    Box(
        modifier = Modifier
            .width(100.dp) // Ancho aumentado para acomodar íconos y padding cómodamente
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .height(82.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onClick)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Box de 36x46dp (simula hoja)
            Box(
                modifier = Modifier
                    .size(36.dp, 46.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8EEF7)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$numero",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Encima del Box: Acciones rápidas (Alineadas arriba-derecha del contenedor)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ActionIconButton(icon = Icons.Default.Delete, onClick = { Toast.makeText(context, "¿Eliminar página $numero?", Toast.LENGTH_SHORT).show() })
            ActionIconButton(icon = Icons.Default.ContentCopy, onClick = { Toast.makeText(context, "Página $numero duplicada", Toast.LENGTH_SHORT).show() })
            ActionIconButton(icon = Icons.Default.RotateRight, onClick = { Toast.makeText(context, "Página $numero rotada", Toast.LENGTH_SHORT).show() })
        }
    }
}

@Composable
fun ActionIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(24.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
