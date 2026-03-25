package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhosue.editorpdf.ui.components.BottomSheetCompartir
import com.jhosue.editorpdf.ui.components.BottomSheetGuardar
import com.jhosue.editorpdf.ui.components.DialogoConfirmarSalida
import com.jhosue.editorpdf.ui.components.PanelAnotar
import com.jhosue.editorpdf.ui.components.PanelEditar
import com.jhosue.editorpdf.ui.components.PanelPaginas
import com.jhosue.editorpdf.ui.components.PantallaProgreso
import com.jhosue.editorpdf.ui.navigation.Routes

/**
 * Pantalla de Edición Principal de PDFix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFirma: () -> Unit
) {
    val context = LocalContext.current
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isContentEditActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Estados para diálogos y bottom sheets (Subfase 1.9)
    var mostrarGuardar by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var mostrarConfirmarSalida by remember { mutableStateOf(false) }
    var mostrarProgreso by remember { mutableStateOf(false) }

    // Animación de ancho del panel lateral
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarOpen) 72.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "SidebarWidth"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contrato_empresa.pdf",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { mostrarConfirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // Botones Deshacer/Rehacer desactivados
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Deshacer",
                            modifier = Modifier.alpha(0.3f)
                        )
                    }
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Rehacer",
                            modifier = Modifier.alpha(0.3f)
                        )
                    }
                    // Menú desplegable
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Guardar") },
                                onClick = { showMenu = false; mostrarGuardar = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir") },
                                onClick = { showMenu = false; mostrarCompartir = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Información del archivo") },
                                onClick = { showMenu = false; mostrarProgreso = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            // Barra de herramientas inferior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("ANOTAR", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("EDITAR", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("PÁGINAS", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                // Panel dinámico según el tab seleccionado
                when (selectedTab) {
                    0 -> PanelAnotar(onNavigateToFirma)
                    1 -> PanelEditar(
                        isActive = isContentEditActive,
                        onActiveChange = { isContentEditActive = it }
                    )
                    2 -> PanelPaginas()
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 3a. PANEL LATERAL DE MINIATURAS
                Box(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                ) {
                    if (isSidebarOpen) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items((1..8).toList()) { page ->
                                ThumbnailItem(page = page, isSelected = page == 1)
                            }
                        }
                    }
                }

                // 3c. ÁREA PDF
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items((1..3).toList()) { _ ->
                            PdfPageSimulation(isEditActive = isContentEditActive)
                        }
                    }

                    // Indicador flotante de modo edición
                    if (isContentEditActive) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = "✏ Modo edición activo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 3d. INDICADOR DE PÁGINA
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Página 1 de 3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // 3e. BOTONES DE ZOOM
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZoomButton(icon = Icons.Default.Add, onClick = { Toast.makeText(context, "Zoom +", Toast.LENGTH_SHORT).show() })
                        ZoomButton(icon = Icons.Default.Remove, onClick = { Toast.makeText(context, "Zoom -", Toast.LENGTH_SHORT).show() })
                    }
                }
            }

            // 3b. BOTÓN TOGGLE DEL PANEL
            IconButton(
                onClick = { isSidebarOpen = !isSidebarOpen },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = sidebarWidth - 12.dp)
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = if (isSidebarOpen) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                    contentDescription = "Toggle Panel",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // --- COMPONENTES DE SALIDA Y FEEDBACK (Subfase 1.9) ---

        if (mostrarGuardar) {
            BottomSheetGuardar(onDismiss = { mostrarGuardar = false })
        }

        if (mostrarCompartir) {
            BottomSheetCompartir(onDismissRequest = { mostrarCompartir = false })
        }

        if (mostrarConfirmarSalida) {
            DialogoConfirmarSalida(
                onConfirm = { 
                    mostrarConfirmarSalida = false
                    onNavigateBack() 
                },
                onDismiss = { mostrarConfirmarSalida = false }
            )
        }

        if (mostrarProgreso) {
            PantallaProgreso(onCancel = { mostrarProgreso = false })
        }
    }
}

/**
 * Componente que simula una miniatura de página en el panel lateral
 */
@Composable
fun ThumbnailItem(page: Int, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(56.dp, 72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = page.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Componente que simula una página A4 con líneas de texto
 */
@Composable
fun PdfPageSimulation(isEditActive: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f)
            .then(
                if (isEditActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(4.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Generamos 8 líneas de "texto" simulado
            val lineWidths = listOf(0.95f, 0.90f, 0.85f, 0.70f, 0.92f, 0.88f, 0.60f, 0.75f)
            lineWidths.forEach { width ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(width)
                        .height(12.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f))
                )
            }
        }
    }
}

/**
 * Botón de zoom personalizado
 */
@Composable
fun ZoomButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Placeholder para herramientas no implementadas aún
 */
@Composable
fun ToolPlaceholder(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
