package com.jhosue.editorpdf.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.jhosue.editorpdf.ui.components.BottomSheetCompartir
import com.jhosue.editorpdf.ui.components.CompartirCallbacks
import com.jhosue.editorpdf.ui.components.BottomSheetGuardar
import com.jhosue.editorpdf.ui.components.DialogoConfirmarSalida
import com.jhosue.editorpdf.ui.components.FormatCallbacks
import com.jhosue.editorpdf.ui.components.GuardarCallbacks
import com.jhosue.editorpdf.ui.components.PanelAnotar
import com.jhosue.editorpdf.ui.components.AnnotationCallbacks
import com.jhosue.editorpdf.ui.components.PanelEditar
import com.jhosue.editorpdf.ui.components.PanelPaginas
import com.jhosue.editorpdf.ui.components.PantallaProgreso
import com.jhosue.editorpdf.ui.components.PdfViewer
import com.jhosue.editorpdf.ui.components.PdfViewerCallbacks
import com.jhosue.editorpdf.ui.components.LayerRenderer
import com.jhosue.editorpdf.ui.components.SnackbarPDFix
import com.jhosue.editorpdf.ui.components.SnackbarTipo
import com.jhosue.editorpdf.ui.navigation.Routes
import com.jhosue.editorpdf.viewmodel.EditorViewModel
import com.jhosue.editorpdf.viewmodel.FirmaViewModel
import com.jhosue.editorpdf.viewmodel.GuardarEstado
import com.jhosue.editorpdf.data.models.PdfState
import com.jhosue.editorpdf.utils.CoordinateMapper
import kotlinx.coroutines.flow.StateFlow

/**
 * Pantalla de Edición Principal de PDFix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    pdfUri: Uri?,
    onNavigateBack: () -> Unit,
    onNavigateToFirma: () -> Unit,
    savedStateHandle: SavedStateHandle? = null
) {
    val context = LocalContext.current
    val viewModel: EditorViewModel = viewModel()
    val firmaViewModel: FirmaViewModel = viewModel()

    // Estados del ViewModel colectados como estado Compose
    val pdfState by viewModel.pdfState.collectAsState()
    val bitmap by viewModel.bitmapActual.collectAsState()
    val paginaActual by viewModel.paginaActual.collectAsState()
    val totalPaginas by viewModel.totalPaginas.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val offsetY by viewModel.offsetY.collectAsState()
    val layerData by viewModel.layerActual.collectAsState()
    val pdfPageWidth by viewModel.pdfPageWidth.collectAsState()
    val pdfPageHeight by viewModel.pdfPageHeight.collectAsState()
    val herramientaActiva by viewModel.herramientaActivaAnotar.collectAsState()
    val colorAnotacion by viewModel.colorAnotacionActual.collectAsState()
    val grosorDibujo by viewModel.grosorDibujo.collectAsState()
    val formaSeleccionada by viewModel.formaSeleccionada.collectAsState()
    val textBlocks by viewModel.textBlocksActuales.collectAsState()
    val bloqueSeleccionado by viewModel.bloqueSeleccionado.collectAsState()
    val modoEdicionActivo by viewModel.modoEdicionActivo.collectAsState()
    val guardarEstado by viewModel.guardarEstado.collectAsState()
    val mostrarDialogoGuardarAntesDeCompartir by viewModel.mostrarDialogoGuardarAntesDeCompartir.collectAsState()

    var isSidebarOpen by remember { mutableStateOf(false) }
    var isContentEditActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    // Estados para colocación de firma
    var modoColocacionFirma by remember { mutableStateOf(false) }
    var firmaBitmapActual by remember { mutableStateOf<Bitmap?>(null) }

    // Estados para diálogos y bottom sheets
    var mostrarGuardar by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var mostrarConfirmarSalida by remember { mutableStateOf(false) }
    var mostrarProgreso by remember { mutableStateOf(false) }

    // Estados para edición de texto
    var textoEditado by remember { mutableStateOf("") }
    var mostrarDialogoEdicionTexto by remember { mutableStateOf(false) }

    // OPTIMIZACIÓN: Memorizar callbacks para evitar recomposiciones innecesarias
    // Al memorizar los callbacks, evitamos que PanelAnotar y PanelEditar
    // se recomongan cada vez que EditorScreen se recomone
    val annotationCallbacks = remember {
        AnnotationCallbacks(
            onToolSelected = { viewModel.setHerramientaAnotacion(it) },
            onColorSelected = { viewModel.setColorAnotacion(it) },
            onThicknessSelected = { viewModel.setGrosorDibujo(it) },
            onShapeSelected = { viewModel.setFormaSeleccionada(it) }
        )
    }

    val formatCallbacks = remember {
        FormatCallbacks(
            onFuenteSelected = { viewModel.setFuenteSeleccionada(it) },
            onTamanioSelected = { viewModel.setTamanioSeleccionado(it) },
            onNegritaChanged = { viewModel.setEsNegrita(it) },
            onCursivaChanged = { viewModel.setEsCursiva(it) },
            onColorSelected = { viewModel.setColorTexto(it) },
            onAlineacionSelected = { viewModel.setAlineacionTexto(it) }
        )
    }

    // Efecto para mostrar diálogo cuando se selecciona un bloque en modo TEXTO
    LaunchedEffect(bloqueSeleccionado) {
        if (bloqueSeleccionado != null && herramientaActiva == "TEXTO") {
            textoEditado = bloqueSeleccionado?.texto ?: ""
            mostrarDialogoEdicionTexto = true
        }
    }

    // Efecto para observar firmaId del savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            val firmaId = handle.get<Int>("firmaId")
            if (firmaId != null && firmaId > 0) {
                val bitmap = firmaViewModel.obtenerBitmapFirma(firmaId)
                if (bitmap != null) {
                    firmaBitmapActual = bitmap
                    modoColocacionFirma = true
                    viewModel.setHerramientaAnotacion("FIRMA")
                }
                handle.remove<Int>("firmaId")
            }
        }
    }

    // Abrir el PDF cuando se proporciona una URI
    LaunchedEffect(pdfUri) {
        pdfUri?.let {
            viewModel.abrirPdf(it)
        }
    }

    // Cargar textBlocks cuando se activa el modo edición
    LaunchedEffect(modoEdicionActivo, paginaActual) {
        if (modoEdicionActivo && pdfState is PdfState.Success) {
            viewModel.cargarTextBlocks()
        }
    }

    // Efecto para observar estado de guardado
    LaunchedEffect(guardarEstado) {
        when (guardarEstado) {
            is GuardarEstado.Error -> {
                // El snackbar se muestra abajo
            }
            else -> {}
        }
    }

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
                        text = fileName.ifEmpty { "Sin archivo" },
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
                    // Botón Deshacer
                    val puedeDeshacer by viewModel.undoRedoManager.puedeDeshacer.collectAsState()
                    IconButton(
                        onClick = { viewModel.deshacer() },
                        enabled = puedeDeshacer
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            modifier = Modifier.alpha(if (puedeDeshacer) 1f else 0.3f),
                            tint = if (puedeDeshacer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    // Botón Rehacer
                    val puedeRehacer by viewModel.undoRedoManager.puedeRehacer.collectAsState()
                    IconButton(
                        onClick = { viewModel.rehacer() },
                        enabled = puedeRehacer
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            modifier = Modifier.alpha(if (puedeRehacer) 1f else 0.3f),
                            tint = if (puedeRehacer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                                onClick = { showMenu = false; viewModel.compartir(context) }
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
                    .navigationBarsPadding()
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
                    0 -> PanelAnotar(
                        onNavigateToFirma = onNavigateToFirma,
                        callbacks = annotationCallbacks
                    )
                    1 -> PanelEditar(
                        isActive = modoEdicionActivo,
                        onActiveChange = {
                            isContentEditActive = it
                            viewModel.setModoEdicionActivo(it)
                        },
                        formatCallbacks = formatCallbacks
                    )
                    2 -> PanelPaginas(viewModel = viewModel)
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
                    when (val state = pdfState) {
                        is PdfState.Idle -> {
                            // Estado inicial - placeholder
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .alpha(0.3f),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Selecciona un PDF para editar",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        is PdfState.Loading -> {
                            // Estado de carga
                            PantallaProgreso(onCancel = { viewModel.cerrarPdf() })
                        }
                        is PdfState.Error -> {
                            // Estado de error
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = state.mensaje,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.cerrarPdf(); onNavigateBack() }
                                ) {
                                    Text("Volver")
                                }
                            }
                        }
                        is PdfState.Success -> {
                            // PDF cargado exitosamente - mostrar visor con capa de anotaciones
                            Box(modifier = Modifier.fillMaxSize()) {
                                PdfViewer(
                                    bitmap = bitmap,
                                    paginaActual = paginaActual,
                                    totalPaginas = totalPaginas,
                                    zoomLevel = zoomLevel,
                                    offsetY = offsetY,
                                    herramientaActiva = herramientaActiva,
                                    colorAnotacion = colorAnotacion,
                                    grosorDibujo = grosorDibujo,
                                    formaSeleccionada = formaSeleccionada,
                                    onZoomChange = { viewModel.setZoomLevel(it) },
                                    onOffsetChange = { viewModel.setOffsetY(it) },
                                    onSiguientePagina = { viewModel.siguientePagina() },
                                    onAnteriorPagina = { viewModel.anteriorPagina() },
                                    callbacks = PdfViewerCallbacks(
                                        onTap = { offset, w, h ->
                                            if (modoColocacionFirma) {
                                                val firma = firmaBitmapActual
                                                if (firma != null) {
                                                    val pdfPoint = CoordinateMapper.screenToPdf(
                                                        offset, w, h, pdfPageWidth, pdfPageHeight,
                                                        zoomLevel, 0f, offsetY
                                                    )
                                                    viewModel.agregarFirmaDesdeBitmap(
                                                        firma,
                                                        pdfPoint,
                                                        1.0f
                                                    )
                                                    modoColocacionFirma = false
                                                    firmaBitmapActual = null
                                                    viewModel.setHerramientaAnotacion(null)
                                                    Toast.makeText(context, "Firma insertada", Toast.LENGTH_SHORT).show()
                                                }
                                            } else if (modoEdicionActivo) {
                                                // En modo edición, buscar bloque de texto en el punto
                                                val bloque = viewModel.buscarBloqueEnPunto(offset, w, h)
                                                if (bloque != null) {
                                                    viewModel.seleccionarBloque(bloque)
                                                    Toast.makeText(context, "Bloque seleccionado: ${bloque.texto.take(30)}...", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.seleccionarBloque(null)
                                                }
                                            } else {
                                                viewModel.procesarToqueAnotacion(offset, w, h)
                                            }
                                        },
                                        onDrawingStart = {},
                                        onDrawingUpdate = {},
                                        onDrawingEnd = { puntos ->
                                            viewModel.agregarDibujoLibre(puntos)
                                        },
                                        onShapeStart = {},
                                        onShapeUpdate = {},
                                        onShapeEnd = { inicio, fin ->
                                            viewModel.agregarForma(inicio, fin)
                                        },
                                        onTextoLibreConfirm = { offset, w, h, texto ->
                                            val pdfPoint = CoordinateMapper.screenToPdf(
                                                offset, w, h, pdfPageWidth, pdfPageHeight,
                                                zoomLevel, 0f, offsetY
                                            )
                                            viewModel.agregarTextoLibre(pdfPoint, texto)
                                        },
                                        onNotaConfirm = { offset, w, h, texto ->
                                            val pdfPoint = CoordinateMapper.screenToPdf(
                                                offset, w, h, pdfPageWidth, pdfPageHeight,
                                                zoomLevel, 0f, offsetY
                                            )
                                            viewModel.agregarNotaAdhesiva(pdfPoint, texto)
                                        }
                                    )
                                )
                                
                                // Capa de anotaciones superpuesta sobre el PDF
                                LayerRenderer(
                                    layerData = layerData,
                                    screenWidth = 1080f,
                                    screenHeight = bitmap?.height?.toFloat() ?: 0f,
                                    pdfPageWidth = pdfPageWidth,
                                    pdfPageHeight = pdfPageHeight,
                                    zoom = zoomLevel,
                                    offsetX = 0f,
                                    offsetY = offsetY,
                                    textBlocks = textBlocks,
                                    bloqueSeleccionado = bloqueSeleccionado,
                                    colorPrimario = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Indicador flotante de modo edición
                            if (modoEdicionActivo) {
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

                            // BOTONES DE ZOOM
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ZoomButton(
                                    icon = Icons.Default.Add,
                                    onClick = { viewModel.agregarZoom(0.25f) }
                                )
                                ZoomButton(
                                    icon = Icons.Default.Remove,
                                    onClick = { viewModel.agregarZoom(-0.25f) }
                                )
                            }
                        }
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
            BottomSheetGuardar(
                onDismiss = { mostrarGuardar = false },
                callbacks = GuardarCallbacks(
                    onGuardar = { viewModel.guardar() },
                    onGuardarComo = { nombre -> viewModel.guardarComo(nombre) }
                )
            )
        }

        if (mostrarCompartir) {
            BottomSheetCompartir(
                onDismissRequest = { mostrarCompartir = false },
                callbacks = CompartirCallbacks(
                    onCompartir = {
                        mostrarCompartir = false
                        viewModel.compartirDirecto(context)
                    }
                )
            )
        }

        // Diálogo para guardar antes de compartir
        if (mostrarDialogoGuardarAntesDeCompartir) {
            AlertDialog(
                onDismissRequest = { viewModel.cerrarDialogoGuardarAntesDeCompartir() },
                title = { Text("Compartir PDF") },
                text = { Text("¿Deseas guardar los cambios antes de compartir?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.guardarYCompartir(context)
                        }
                    ) {
                        Text("Guardar y compartir")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.compartirDirecto(context)
                            viewModel.cerrarDialogoGuardarAntesDeCompartir()
                        }
                    ) {
                        Text("Compartir sin guardar")
                    }
                }
            )
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

        // Mostrar pantalla de progreso durante guardado
        if (guardarEstado is GuardarEstado.Guardando) {
            PantallaProgreso(
                progreso = 0.5f,
                onCancel = { viewModel.resetearGuardarEstado() }
            )
        }

        // Mostrar snackbar según resultado del guardado
        if (guardarEstado is GuardarEstado.Exito) {
            SnackbarPDFix(
                mensaje = "PDF guardado exitosamente",
                tipo = SnackbarTipo.EXITO,
                onDismissRequest = { viewModel.resetearGuardarEstado() }
            )
        }

        if (guardarEstado is GuardarEstado.ExitoGuardarComo) {
            SnackbarPDFix(
                mensaje = "Guardado en: ${(guardarEstado as GuardarEstado.ExitoGuardarComo).ruta}",
                tipo = SnackbarTipo.EXITO,
                onDismissRequest = { viewModel.resetearGuardarEstado() }
            )
        }

        if (guardarEstado is GuardarEstado.Error) {
            SnackbarPDFix(
                mensaje = (guardarEstado as GuardarEstado.Error).mensaje,
                tipo = SnackbarTipo.ERROR,
                onDismissRequest = { viewModel.resetearGuardarEstado() }
            )
        }

        // Diálogo de edición de texto
        if (mostrarDialogoEdicionTexto) {
            val bloque = bloqueSeleccionado
            if (bloque != null) {
                DialogoEditarTexto(
                    textoActual = textoEditado,
                    textoOriginal = bloque.texto,
                    onTextoChange = { textoEditado = it },
                    onConfirmar = {
                        viewModel.editarTextoBloque(bloque, textoEditado)
                        mostrarDialogoEdicionTexto = false
                        textoEditado = ""
                    },
                    onDismiss = {
                        mostrarDialogoEdicionTexto = false
                        viewModel.seleccionarBloque(null)
                        textoEditado = ""
                    }
                )
            }
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

/**
 * Diálogo para editar el texto de un bloque seleccionado.
 * Permite modificar el texto y mostrar advertencias si el texto nuevo es muy largo.
 * @param textoActual Texto que se está editando actualmente.
 * @param textoOriginal Texto original del bloque para comparar longitud.
 * @param onTextoChange Callback cuando el texto cambia.
 * @param onConfirmar Callback cuando se confirma la edición.
 * @param onDismiss Callback cuando se cierra el diálogo.
 */
@Composable
fun DialogoEditarTexto(
    textoActual: String,
    textoOriginal: String,
    onTextoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    // Umbral de advertencia: si el texto nuevo es más del doble de largo
    val mostrarAdvertencia = textoActual.length > textoOriginal.length * 2
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Editar texto",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textoActual,
                    onValueChange = onTextoChange,
                    label = { Text("Texto") },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (mostrarAdvertencia) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "El texto nuevo es mucho más largo. Podría desbordar el espacio disponible.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmar) {
                Text("Aplicar cambio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
