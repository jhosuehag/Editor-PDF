package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhosue.editorpdf.ui.components.PantallaProgreso
import com.jhosue.editorpdf.viewmodel.UnirPdfEstado
import com.jhosue.editorpdf.viewmodel.UnirPdfViewModel

/**
 * Pantalla para unir múltiples archivos PDF en uno solo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnirPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: UnirPdfViewModel = viewModel()
) {
    val context = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    val archivosSeleccionados by viewModel.archivosSeleccionados.collectAsState()
    val nombresArchivos by viewModel.nombresArchivos.collectAsState()

    var nombreDestino by remember { mutableStateOf("documento_unido") }
    var showDialogoNombre by remember { mutableStateOf(false) }

    // Lanzador para seleccionar múltiples archivos PDF
    val launcherPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.agregarArchivos(uris)
        }
    }

    // Mostrar Toast según estado
    LaunchedEffect(estado) {
        when (estado) {
            is UnirPdfEstado.Exito -> {
                Toast.makeText(context, "PDF creado exitosamente", Toast.LENGTH_LONG).show()
            }
            is UnirPdfEstado.Error -> {
                Toast.makeText(context, (estado as UnirPdfEstado.Error).mensaje, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
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
                    viewModel.unirPdfs(nombreDestino)
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

    // Pantalla de progreso
    if (estado is UnirPdfEstado.Procesando) {
        val estadoProceso = estado as UnirPdfEstado.Procesando
        PantallaProgreso(
            progreso = estadoProceso.progreso,
            onCancel = { viewModel.resetearEstado() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unir PDFs", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (archivosSeleccionados.isNotEmpty()) {
                        IconButton(onClick = { viewModel.limpiarArchivos() }) {
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
                text = "Archivos a unir (${archivosSeleccionados.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (archivosSeleccionados.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Agrega al menos 2 PDFs para unir",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    itemsIndexed(nombresArchivos) { indice, nombre ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${indice + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.eliminarArchivo(indice) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { launcherPdf.launch(arrayOf("application/pdf")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("+ Agregar archivos PDF")
            }

            Button(
                onClick = {
                    if (archivosSeleccionados.size < 2) {
                        Toast.makeText(context, "Se necesitan al menos 2 archivos", Toast.LENGTH_SHORT).show()
                    } else {
                        showDialogoNombre = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = archivosSeleccionados.size >= 2 && estado !is UnirPdfEstado.Procesando
            ) {
                Text("Unir y guardar →")
            }
        }
    }

    // Botón para abrir PDF resultante si hubo éxito
    if (estado is UnirPdfEstado.Exito) {
        val estadoExito = estado as UnirPdfEstado.Exito
        LaunchedEffect(Unit) {
            // Mostrar diálogo ofreciendo abrir
            Toast.makeText(context, "Guardado en: ${estadoExito.rutaArchivo}", Toast.LENGTH_LONG).show()
            viewModel.resetearEstado()
        }
    }
}