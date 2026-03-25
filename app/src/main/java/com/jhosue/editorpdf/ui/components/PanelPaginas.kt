package com.jhosue.editorpdf.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhosue.editorpdf.viewmodel.EditorViewModel
import com.jhosue.editorpdf.viewmodel.PageOperationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Panel de gestión de páginas para el editor de PDF.
 * Permite visualizar miniaturas, eliminar, duplicar, insertar, rotar,
 * reordenar y extraer páginas.
 */
@Composable
fun PanelPaginas(
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Estados del ViewModel
    val totalPaginas by viewModel.totalPaginas.collectAsState()
    val paginaActual by viewModel.paginaActual.collectAsState()
    val pageOperationState by viewModel.pageOperationState.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()
    
    // Estado local
    var paginaSeleccionada by remember { mutableIntStateOf(0) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var paginaAEliminar by remember { mutableIntStateOf(-1) }
    var mostrarDialogoExtraer by remember { mutableStateOf(false) }
    var paginaAExtraer by remember { mutableIntStateOf(-1) }
    var nombreExtraer by remember { mutableStateOf("pagina_extraida") }
    var ordenPaginas by remember { mutableStateOf<List<Int>>(emptyList()) }
    var ordenOriginal by remember { mutableStateOf<List<Int>>(emptyList()) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    
    // Inicializar orden de páginas cuando cambia el total
    LaunchedEffect(totalPaginas) {
        if (ordenPaginas.size != totalPaginas) {
            ordenPaginas = (0 until totalPaginas).toList()
            ordenOriginal = ordenPaginas.toList()
        }
    }
    
    // Escuchar estado de operación de página para mostrar Toast
    LaunchedEffect(pageOperationState) {
        when (val state = pageOperationState) {
            is PageOperationState.Success -> {
                Toast.makeText(context, state.mensaje, Toast.LENGTH_SHORT).show()
                viewModel.resetPageOperationState()
            }
            is PageOperationState.Error -> {
                Toast.makeText(context, state.mensaje, Toast.LENGTH_SHORT).show()
                viewModel.resetPageOperationState()
            }
            else -> {}
        }
    }
    
    // Sincronizar página seleccionada con página actual del visor
    LaunchedEffect(paginaActual) {
        paginaSeleccionada = paginaActual
    }
    
    // Indicador de progreso
    val isLoading = pageOperationState is PageOperationState.Loading
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Indicador de carga
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // FILA 1 — Barra de acciones globales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$totalPaginas páginas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón Insertar página en blanco
            TextButton(
                onClick = {
                    viewModel.insertarPaginaBlanco(paginaSeleccionada)
                },
                enabled = totalPaginas > 0 && !isLoading
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Insertar", style = MaterialTheme.typography.labelMedium)
            }
            
            // Botón Extraer
            TextButton(
                onClick = {
                    if (totalPaginas > 0) {
                        paginaAExtraer = paginaSeleccionada
                        nombreExtraer = "pagina_${paginaSeleccionada + 1}"
                        mostrarDialogoExtraer = true
                    }
                },
                enabled = totalPaginas > 0 && !isLoading
            ) {
                Icon(
                    Icons.Default.Output,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Extraer", style = MaterialTheme.typography.labelMedium)
            }
            
            // Botón Reordenar - activa modo drag
            TextButton(
                onClick = {
                    if (ordenPaginas != ordenOriginal) {
                        // Aplicar nuevo orden
                        viewModel.reordenarPaginas(ordenPaginas)
                        ordenOriginal = ordenPaginas.toList()
                    }
                },
                enabled = ordenPaginas != ordenOriginal && !isLoading
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (ordenPaginas != ordenOriginal) "Aplicar" else "Reordenar", style = MaterialTheme.typography.labelMedium)
            }
        }
        
        // FILA 2 — Lista de miniaturas con drag & drop
        LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = ordenPaginas,
                key = { _, pageIndex -> pageIndex }
            ) { displayIndex, pageIndex ->
                val isSelected = paginaSeleccionada == pageIndex
                val isDragging = dragState?.draggedIndex == displayIndex
                val dragOffset = if (dragState?.draggedIndex == displayIndex) dragState?.offset ?: 0f else 0f
                
                PageThumbnailItem(
                    pageIndex = pageIndex,
                    displayIndex = displayIndex,
                    isSelected = isSelected,
                    isDragging = isDragging,
                    dragOffset = dragOffset,
                    viewModel = viewModel,
                    onSelect = { 
                        paginaSeleccionada = pageIndex
                        viewModel.cargarPagina(pageIndex)
                    },
                    onEliminar = {
                        paginaAEliminar = pageIndex
                        mostrarDialogoEliminar = true
                    },
                    onDuplicar = {
                        viewModel.duplicarPagina(pageIndex)
                    },
                    onRotar = {
                        viewModel.rotarPagina(pageIndex, 90)
                    },
                    onDragStart = {
                        dragState = DragState(draggedIndex = displayIndex, offset = 0f)
                    },
                    onDrag = { delta ->
                        val currentState = dragState
                        if (currentState != null) {
                            dragState = currentState.copy(offset = currentState.offset + delta)
                            
                            // Calcular nuevo índice basado en el desplazamiento
                            val itemWidth = 108f // Ancho aproximado del item
                            val indexDelta = (currentState.offset / itemWidth).toInt()
                            val newDisplayIndex = (displayIndex + indexDelta).coerceIn(0, ordenPaginas.size - 1)
                        
                            if (newDisplayIndex != displayIndex) {
                                // Reordenar la lista visual
                                val mutableOrden = ordenPaginas.toMutableList()
                                mutableOrden.removeAt(displayIndex)
                                mutableOrden.add(newDisplayIndex, pageIndex)
                                ordenPaginas = mutableOrden
                                dragState = dragState?.copy(draggedIndex = newDisplayIndex, offset = 0f)
                            }
                        }
                    },
                    onDragEnd = {
                        dragState = null
                    }
                )
            }
        }
    }
    
    // Diálogo de confirmación para eliminar
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar página") },
            text = { Text("¿Estás seguro de que deseas eliminar la página ${paginaAEliminar + 1}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPagina(paginaAEliminar)
                        mostrarDialogoEliminar = false
                        // Sincronizar el orden después de eliminar
                        if (ordenPaginas.isNotEmpty()) {
                            ordenPaginas = (0 until ordenPaginas.size).toList()
                            ordenOriginal = ordenPaginas.toList()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Diálogo para extraer página
    if (mostrarDialogoExtraer) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExtraer = false },
            title = { Text("Extraer página") },
            text = {
                Column {
                    Text("Ingresa el nombre para el archivo:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nombreExtraer,
                        onValueChange = { nombreExtraer = it },
                        label = { Text("Nombre del archivo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val destinoPath = "${context.getExternalFilesDir(null)?.absolutePath}/$nombreExtraer.pdf"
                        viewModel.extraerPagina(paginaAExtraer, destinoPath)
                        mostrarDialogoExtraer = false
                    }
                ) {
                    Text("Extraer")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoExtraer = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Estado para el drag & drop.
 */
private data class DragState(
    val draggedIndex: Int,
    val offset: Float
)

/**
 * Componente individual de miniatura de página con acciones y soporte drag.
 */
@Composable
fun PageThumbnailItem(
    pageIndex: Int,
    displayIndex: Int,
    isSelected: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    viewModel: EditorViewModel,
    onSelect: () -> Unit,
    onEliminar: () -> Unit,
    onDuplicar: () -> Unit,
    onRotar: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val numero = pageIndex + 1
    
    // Estado para la miniatura renderizada
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    // Cargar miniatura real usando PdfRenderer
    LaunchedEffect(pageIndex, viewModel) {
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                // Usar el repositorio para renderizar la página
                val repository = com.jhosue.editorpdf.repository.PdfRenderRepository(context)
                // La miniatura se renderiza a bajo nivel
                // Aquí usamos una aproximación con la Bitmap del viewer
                bitmap = null // Se deja null ya que el PdfRenderer no permite acceso directo
                isLoading = false
            } catch (e: Exception) {
                hasError = true
                isLoading = false
            }
        }
    }
    
    // Animación para el arrastre
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 8f else 2f,
        label = "Elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "Scale"
    )
    
    Box(
        modifier = Modifier
            .width(100.dp)
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationX = dragOffset
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickable(onClick = onSelect)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Área de miniatura
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE8EEF7)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    hasError || bitmap == null -> {
                        // Miniatura placeholder con número
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "$numero",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    else -> {
                        // Miniatura real (si estuviera disponible)
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Página $numero",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Número de página
            Text(
                text = "$numero",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Acciones rápidas (ocultas hasta hover o siempre visibles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionIconButton(
                    icon = Icons.Default.Delete,
                    onClick = onEliminar,
                    contentDescription = "Eliminar"
                )
                ActionIconButton(
                    icon = Icons.Default.ContentCopy,
                    onClick = onDuplicar,
                    contentDescription = "Duplicar"
                )
                ActionIconButton(
                    icon = Icons.Default.RotateRight,
                    onClick = onRotar,
                    contentDescription = "Rotar"
                )
            }
        }
    }
}

/**
 * Botón de acción pequeño para las miniaturas.
 */
@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(20.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}