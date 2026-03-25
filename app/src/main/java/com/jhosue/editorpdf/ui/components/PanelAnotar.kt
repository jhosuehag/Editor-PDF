package com.jhosue.editorpdf.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhosue.editorpdf.data.models.ShapeType

/**
 * Callbacks para las acciones de anotación.
 */
data class AnnotationCallbacks(
    val onToolSelected: (String?) -> Unit,
    val onColorSelected: (Int) -> Unit,
    val onThicknessSelected: (Float) -> Unit,
    val onShapeSelected: (ShapeType) -> Unit
)

/**
 * Panel de herramientas de anotación para el editor de PDF.
 */
@Composable
fun PanelAnotar(
    onNavigateToFirma: () -> Unit,
    callbacks: AnnotationCallbacks
) {
    var activeToolIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // PARTE 1 — BARRA DE HERHERRAMIENTAS PRINCIPAL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tools = listOf(
                AnnotationTool("Resaltar", Icons.Default.Highlight, "RESALTAR"),
                AnnotationTool("Subrayar", Icons.Default.FormatUnderlined, "SUBRAYAR"),
                AnnotationTool("Tachar", Icons.Default.FormatStrikethrough, "TACHAR"),
                AnnotationTool("Dibujo", Icons.Default.Brush, "DIBUJO"),
                AnnotationTool("Texto", Icons.Default.TextFields, "TEXTO_LIBRE"),
                AnnotationTool("Nota", Icons.Default.StickyNote2, "NOTA"),
                AnnotationTool("Formas", Icons.Default.Category, "FORMA"),
                AnnotationTool("Firma", Icons.Default.HistoryEdu, "FIRMA")
            )

            tools.forEachIndexed { index, tool ->
                ToolItem(
                    tool = tool,
                    isActive = activeToolIndex == index,
                    onClick = {
                        activeToolIndex = if (activeToolIndex == index) null else index
                        callbacks.onToolSelected(if (activeToolIndex == index) tool.toolName else null)
                    }
                )
            }
        }

        // PARTE 2 — SUB-PANEL DINÁMICO
        AnimatedVisibility(
            visible = activeToolIndex != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                when (activeToolIndex) {
                    0 -> SubPanelResaltar(callbacks.onColorSelected)
                    1, 2 -> SubPanelSubrayarTachar(callbacks.onColorSelected)
                    3 -> SubPanelDibujo(callbacks.onColorSelected, callbacks.onThicknessSelected)
                    4 -> SubPanelTextoNota("Toca el documento para posicionar el texto")
                    5 -> SubPanelTextoNota("Toca donde quieres colocar la nota")
                    6 -> SubPanelFormas(callbacks.onShapeSelected, callbacks.onColorSelected)
                    7 -> SubPanelFirma(onNavigateToFirma)
                }
            }
        }
    }
}

data class AnnotationTool(
    val label: String, 
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val toolName: String
)

@Composable
fun ToolItem(tool: AnnotationTool, isActive: Boolean, onClick: () -> Unit) {
    val contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val bgColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent

    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.label,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.label,
            fontSize = 10.sp,
            color = contentColor
        )
    }
}

@Composable
fun SubPanelResaltar(onColorSelected: (Int) -> Unit) {
    var selectedColor by remember { mutableStateOf(0) }
    val colors = listOf(
        0xFFFFE066.toInt(), // Amarillo
        0xFF86EFAC.toInt(), // Verde
        0xFF93C5FD.toInt(), // Azul
        0xFFFDA4AF.toInt(), // Rosa
        0xFFFCA5A1.toInt(), // Naranja
        0xFFC4B5FD.toInt()  // Lavanda
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { index, colorInt ->
            ColorCircle(
                color = Color(colorInt),
                isSelected = selectedColor == index,
                onClick = { 
                    selectedColor = index
                    onColorSelected(colorInt)
                },
                size = 32.dp
            )
        }
    }
}

@Composable
fun SubPanelSubrayarTachar(onColorSelected: (Int) -> Unit) {
    var selectedColor by remember { mutableStateOf(0) }
    val colors = listOf(
        0xFF1E293B.toInt(), // Negro azulado
        0xFFEF4444.toInt(), // Rojo
        0xFF3B82F6.toInt(), // Azul
        0xFF22C55E.toInt()  // Verde
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Color:", style = MaterialTheme.typography.labelMedium)
        colors.forEachIndexed { index, colorInt ->
            ColorCircle(
                color = Color(colorInt),
                isSelected = selectedColor == index,
                onClick = { 
                    selectedColor = index
                    onColorSelected(colorInt)
                },
                size = 28.dp
            )
        }
    }
}

@Composable
fun SubPanelDibujo(
    onColorSelected: (Int) -> Unit,
    onThicknessSelected: (Float) -> Unit
) {
    var selectedThickness by remember { mutableStateOf(1) }
    var selectedColor by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    val colors = listOf(
        0xFF000000.toInt(), // Negro
        0xFFFFFFFF.toInt(), // Blanco
        0xFFEF4444.toInt(), // Rojo
        0xFF3B82F6.toInt(), // Azul
        0xFF22C55E.toInt(), // Verde
        0xFFFFE066.toInt(), // Amarillo
        0xFFFFA500.toInt(), // Naranja
        0xFF800080.toInt()  // Púrpura
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Grosor:", style = MaterialTheme.typography.labelMedium)
            listOf(2f, 4f, 7f).forEachIndexed { index, thickness ->
                ThicknessButton(
                    thickness = thickness,
                    isSelected = selectedThickness == index,
                    onClick = { 
                        selectedThickness = index
                        onThicknessSelected(thickness)
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color:", style = MaterialTheme.typography.labelMedium)
            colors.forEachIndexed { index, colorInt ->
                ColorCircle(
                    color = Color(colorInt),
                    isSelected = selectedColor == index,
                    onClick = { 
                        selectedColor = index
                        onColorSelected(colorInt)
                    },
                    size = 24.dp
                )
            }
        }
    }
}

@Composable
fun SubPanelTextoNota(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Info, 
            contentDescription = null, 
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SubPanelFormas(
    onShapeSelected: (ShapeType) -> Unit,
    onColorSelected: (Int) -> Unit
) {
    var selectedShape by remember { mutableStateOf(1) } // Rectángulo por defecto
    var selectedColor by remember { mutableStateOf(0) }
    
    val shapes = listOf(
        Icons.Default.ArrowForward to ShapeType.FLECHA,
        Icons.Default.Rectangle to ShapeType.RECTANGULO,
        Icons.Default.Circle to ShapeType.CIRCULO,
        Icons.Default.HorizontalRule to ShapeType.LINEA
    )
    
    val colors = listOf(
        0xFF1E293B.toInt(), // Negro
        0xFFEF4444.toInt(), // Rojo
        0xFF3B82F6.toInt(), // Azul
        0xFF22C55E.toInt()  // Verde
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shapes.forEachIndexed { index, (icon, shapeType) ->
            IconButton(
                onClick = { 
                    selectedShape = index
                    onShapeSelected(shapeType)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedShape == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(icon, contentDescription = null, tint = if (selectedShape == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Text("Color:", style = MaterialTheme.typography.labelMedium)
        
        colors.forEachIndexed { index, colorInt ->
            ColorCircle(
                color = Color(colorInt),
                isSelected = selectedColor == index,
                onClick = { 
                    selectedColor = index
                    onColorSelected(colorInt)
                },
                size = 24.dp
            )
        }
    }
}

@Composable
fun SubPanelFirma(onNavigateToFirma: () -> Unit) {
    Text(
        text = "Ir a firma →",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onNavigateToFirma() },
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ThicknessButton(thickness: Float, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp, 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(thickness.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        )
    }
}

/**
 * Callbacks para las acciones de formato de texto.
 */
data class FormatCallbacks(
    val onFuenteSelected: (String) -> Unit,
    val onTamanioSelected: (Float) -> Unit,
    val onNegritaChanged: (Boolean) -> Unit,
    val onCursivaChanged: (Boolean) -> Unit,
    val onColorSelected: (Color) -> Unit,
    val onAlineacionSelected: (String) -> Unit
)
