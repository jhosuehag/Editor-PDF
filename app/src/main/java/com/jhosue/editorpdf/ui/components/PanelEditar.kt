package com.jhosue.editorpdf.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Panel de edición de contenido para el editor de PDF.
 */
@Composable
fun PanelEditar(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    formatCallbacks: FormatCallbacks? = null
) {
    var herramientaActiva by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!isActive) {
            // ESTADO INACTIVO: Botón para activar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { onActiveChange(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Activar edición de contenido")
                }
            }
        } else {
            // ESTADO ACTIVO: Barra de herramientas y sub-paneles
            Column {
                // Fila 1 — Barra de herramientas de edición
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditToolButton(
                        label = "Texto",
                        icon = Icons.Default.EditNote,
                        isSelected = herramientaActiva == "TEXTO",
                        onClick = { herramientaActiva = "TEXTO" }
                    )
                    EditToolButton(
                        label = "Reemplazar",
                        icon = Icons.Default.Image,
                        isSelected = herramientaActiva == "REEMPLAZAR_IMG",
                        onClick = { herramientaActiva = "REEMPLAZAR_IMG" }
                    )
                    EditToolButton(
                        label = "Insertar",
                        icon = Icons.Default.AddPhotoAlternate,
                        isSelected = herramientaActiva == "INSERTAR_IMG",
                        onClick = { herramientaActiva = "INSERTAR_IMG" }
                    )
                    
                    VerticalDivider(
                        modifier = Modifier
                            .height(32.dp)
                            .padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    
                    // Botón Desactivar
                    IconButton(
                        onClick = { 
                            onActiveChange(false)
                            herramientaActiva = null
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Desactivar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Fila 2 — Sub-panel con animación
                AnimatedVisibility(
                    visible = herramientaActiva != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        when (herramientaActiva) {
                            "TEXTO" -> SubPanelTexto(formatCallbacks)
                            "REEMPLAZAR_IMG" -> SubPanelReemplazarImg()
                            "INSERTAR_IMG" -> SubPanelInsertarImg(formatCallbacks)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditToolButton(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = contentColor)
    }
}

@Composable
fun SubPanelTexto(formatCallbacks: FormatCallbacks?) {
    var expanded by remember { mutableStateOf(false) }
    var fontSelected by remember { mutableStateOf("Helvetica") }
    var sizeSelected by remember { mutableStateOf("12") }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedAlineacion by remember { mutableStateOf("LEFT") }

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Fila A — Fuente y tamaño
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fuente:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(8.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(fontSelected, style = MaterialTheme.typography.bodyMedium)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Helvetica", "Times", "Courier").forEach { font ->
                        DropdownMenuItem(
                            text = { Text(font, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { 
                                fontSelected = font
                                formatCallbacks?.onFuenteSelected(font)
                                expanded = false 
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Tamaño:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("10", "12", "14", "16", "18").forEach { size ->
                    SizeChip(
                        size = size, 
                        isSelected = sizeSelected == size, 
                        onClick = { 
                            sizeSelected = size
                            formatCallbacks?.onTamanioSelected(size.toFloat())
                        }
                    )
                }
            }
        }

        // Fila B — Estilo (Negrita y Cursiva)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleIconButton(
                icon = Icons.Default.FormatBold, 
                isSelected = isBold,
                onClick = { 
                    isBold = !isBold
                    formatCallbacks?.onNegritaChanged(isBold)
                }
            )
            StyleIconButton(
                icon = Icons.Default.FormatItalic, 
                isSelected = isItalic,
                onClick = { 
                    isItalic = !isItalic
                    formatCallbacks?.onCursivaChanged(isItalic)
                }
            )
        }

        // Fila C — Alineación y color
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Alineación
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.FormatAlignLeft, contentDescription = null, 
                    modifier = Modifier.size(24.dp).clickable { 
                        selectedAlineacion = "LEFT"
                        formatCallbacks?.onAlineacionSelected("LEFT")
                    },
                    tint = if (selectedAlineacion == "LEFT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Icon(Icons.Default.FormatAlignCenter, contentDescription = null,
                    modifier = Modifier.size(24.dp).clickable { 
                        selectedAlineacion = "CENTER"
                        formatCallbacks?.onAlineacionSelected("CENTER")
                    },
                    tint = if (selectedAlineacion == "CENTER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Icon(Icons.Default.FormatAlignRight, contentDescription = null,
                    modifier = Modifier.size(24.dp).clickable { 
                        selectedAlineacion = "RIGHT"
                        formatCallbacks?.onAlineacionSelected("RIGHT")
                    },
                    tint = if (selectedAlineacion == "RIGHT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Color:", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(8.dp))
            listOf(Color.Black, Color.Gray, Color.Red, Color.Blue, Color.Green).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { 
                            selectedColor = color
                            formatCallbacks?.onColorSelected(color)
                        }
                        .then(
                            if (selectedColor == color) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                )
            }
        }
    }
}

@Composable
fun SubPanelReemplazarImg() {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Toca la imagen en el PDF que deseas reemplazar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SubPanelInsertarImg(formatCallbacks: FormatCallbacks?) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Galería (mock)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(6) { index ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("IMG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun SizeChip(size: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(4.dp),
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
        modifier = Modifier.size(width = 32.dp, height = 28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(size, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StyleIconButton(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
