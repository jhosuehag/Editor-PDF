package com.jhosue.editorpdf.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhosue.editorpdf.ui.components.PantallaProgreso
import com.jhosue.editorpdf.viewmodel.ImagenesPdfEstado
import com.jhosue.editorpdf.viewmodel.ImagenesPdfViewModel

/**
 * Pantalla para convertir una colección de imágenes en un archivo PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagenesPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImagenesPdfViewModel = viewModel()
) {
    val context = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    val imagenesBitmap by viewModel.imagenesBitmap.collectAsState()
    val orientacion by viewModel.orientacion.collectAsState()

    var margenesExpanded by remember { mutableStateOf(false) }
    var margenesSeleccionados by remember { mutableStateOf("Normales") }
    var nombreDestino by remember { mutableStateOf("documento_imagenes") }
    var showDialogoNombre by remember { mutableStateOf(false) }

    // Lanzador para seleccionar múltiples imágenes (Android 13+)
    val launcherImagenes = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.agregarImagenes(uris)
        }
    }

    // Lanzador legacy para Android 12 y anteriores
    val launcherImagenesLegacy = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.agregarImagenes(uris)
        }
    }

    // Mostrar Toast según estado
    LaunchedEffect(estado) {
        when (estado) {
            is ImagenesPdfEstado.Exito -> {
                Toast.makeText(context, "PDF creado exitosamente", Toast.LENGTH_LONG).show()
            }
            is ImagenesPdfEstado.Error -> {
                Toast.makeText(context, (estado as ImagenesPdfEstado.Error).mensaje, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Pantalla de progreso
    if (estado is ImagenesPdfEstado.Procesando) {
        val estadoProceso = estado as ImagenesPdfEstado.Procesando
        PantallaProgreso(
            progreso = estadoProceso.progreso,
            onCancel = { viewModel.resetearEstado() }
        )
    }

    // Diálogo para nombre del archivo
    if (showDialogoNombre) {
        AlertDialog(
            onDismissRequest = { showDialogoNombre = false },
            title = { Text("Nombre del archivo") },
            text = {
                OutlinedTextField(
                    value = nombreDestino,
                    onValueChange = { nombreDestino = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDialogoNombre = false
                    viewModel.convertirAPdf(nombreDestino)
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogoNombre = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Imágenes a PDF", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (imagenesBitmap.isNotEmpty()) {
                        IconButton(onClick = { viewModel.limpiarImagenes() }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar todo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Imágenes seleccionadas (${imagenesBitmap.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // GRID DE MINIATURAS REALES
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (imagenesBitmap.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Agrega imágenes para convertir",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    itemsIndexed(imagenesBitmap) { indice, bitmap ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Imagen ${indice + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { viewModel.eliminarImagen(indice) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcherImagenes.launch(arrayOf("image/*"))
                    } else {
                        launcherImagenesLegacy.launch("image/*")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("+ Agregar imágenes")
            }

            // CONFIGURACIÓN DE PÁGINA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configuración",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Orientación:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Row {
                            FilterChip(
                                selected = orientacion == "VERTICAL",
                                onClick = { viewModel.setOrientacion("VERTICAL") },
                                label = { Text("Vertical") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = orientacion == "HORIZONTAL",
                                onClick = { viewModel.setOrientacion("HORIZONTAL") },
                                label = { Text("Horizontal") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Márgenes:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box {
                            TextButton(onClick = { margenesExpanded = true }) {
                                Text(margenesSeleccionados)
                            }
                            DropdownMenu(
                                expanded = margenesExpanded,
                                onDismissRequest = { margenesExpanded = false }
                            ) {
                                listOf(
                                    "Sin márgenes" to 0f,
                                    "Pequeños" to 10f,
                                    "Normales" to 20f
                                ).forEach { (nombre, valor) ->
                                    DropdownMenuItem(
                                        text = { Text(nombre) },
                                        onClick = {
                                            margenesSeleccionados = nombre
                                            viewModel.setMargen(valor)
                                            margenesExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (imagenesBitmap.isEmpty()) {
                        Toast.makeText(context, "Agrega al menos una imagen", Toast.LENGTH_SHORT).show()
                    } else {
                        showDialogoNombre = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = imagenesBitmap.isNotEmpty() && estado !is ImagenesPdfEstado.Procesando
            ) {
                Text("Convertir a PDF")
            }
        }
    }

    // Resetear estado al成功
    if (estado is ImagenesPdfEstado.Exito) {
        LaunchedEffect(Unit) {
            viewModel.resetearEstado()
        }
    }
}