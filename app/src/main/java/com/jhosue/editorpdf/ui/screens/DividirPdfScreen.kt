package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhosue.editorpdf.ui.components.PantallaProgreso
import com.jhosue.editorpdf.ui.components.RadioButtonRow
import com.jhosue.editorpdf.viewmodel.DividirPdfEstado
import com.jhosue.editorpdf.viewmodel.DividirPdfViewModel
import java.text.DecimalFormat

/**
 * Pantalla para extraer páginas o dividir un PDF por rangos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividirPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: DividirPdfViewModel = viewModel()
) {
    val context = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    val totalPaginas by viewModel.totalPaginas.collectAsState()
    val nombreArchivo by viewModel.nombreArchivo.collectAsState()
    val miniaturas by viewModel.miniaturas.collectAsState()

    var modoDiv by remember { mutableStateOf(0) }
    var desdePagina by remember { mutableStateOf("1") }
    var hastaPagina by remember { mutableStateOf("1") }
    var partesIguales by remember { mutableStateOf("2") }
    var nombreBase by remember { mutableStateOf("documento_dividido") }

    val launcherPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.cargarPdf(it) }
    }

    // Mostrar Toast según estado
    LaunchedEffect(estado) {
        when (estado) {
            is DividirPdfEstado.Exito -> {
                val archivos = (estado as DividirPdfEstado.Exito).archivosCreados
                Toast.makeText(context, "Creados: ${archivos.size} archivo(s)", Toast.LENGTH_LONG).show()
            }
            is DividirPdfEstado.Error -> {
                Toast.makeText(context, (estado as DividirPdfEstado.Error).mensaje, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Pantalla de progreso
    if (estado is DividirPdfEstado.Procesando) {
        val estadoProceso = estado as DividirPdfEstado.Procesando
        PantallaProgreso(
            progreso = estadoProceso.progreso,
            onCancel = { viewModel.resetearEstado() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dividir PDF", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cerrarPdf()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            // INFO DEL PDF ACTUAL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            nombreArchivo.ifEmpty { "Ningún archivo seleccionado" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalPaginas > 0) {
                            val df = DecimalFormat("#.##")
                            Text(
                                "$totalPaginas páginas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    TextButton(
                        onClick = { launcherPdf.launch(arrayOf("application/pdf")) }
                    ) {
                        Text(if (totalPaginas > 0) "Cambiar" else "Seleccionar")
                    }
                }
            }

            if (totalPaginas == 0) {
                // Estado sin PDF cargado
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Selecciona un PDF para dividir",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { launcherPdf.launch(arrayOf("application/pdf")) }
                        ) {
                            Text("Seleccionar PDF")
                        }
                    }
                }
            } else {
                // PDF CARGADO - mostrar opciones de división
                Text(
                    "Selecciona páginas a dividir",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // MINIATURAS REALES
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(miniaturas) { index, bitmap ->
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Página ${index + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Text(
                    "Modo de división",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RadioButtonRow("Por rango de páginas", 0, modoDiv) { modoDiv = it }
                    RadioButtonRow("En partes iguales", 1, modoDiv) { modoDiv = it }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campos según modo
                AnimatedVisibility(visible = modoDiv == 0, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column {
                        Text(
                            "Rango de páginas:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = desdePagina,
                                onValueChange = { desdePagina = it.filter { c -> c.isDigit() } },
                                label = { Text("Desde") },
                                modifier = Modifier.width(90.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Text(" al ", modifier = Modifier.padding(horizontal = 12.dp))
                            OutlinedTextField(
                                value = hastaPagina,
                                onValueChange = { hastaPagina = it.filter { c -> c.isDigit() } },
                                label = { Text("Hasta") },
                                modifier = Modifier.width(90.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Text(
                            "del 1 al $totalPaginas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = modoDiv == 1, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dividir en:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = partesIguales,
                            onValueChange = { partesIguales = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(70.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("partes iguales", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val nombre = nombreArchivo.substringBeforeLast(".")
                        nombreBase = if (nombre.isNotEmpty()) nombre else "documento_dividido"

                        when (modoDiv) {
                            0 -> {
                                val desde = desdePagina.toIntOrNull() ?: 1
                                val hasta = hastaPagina.toIntOrNull() ?: totalPaginas
                                if (desde in 1..totalPaginas && hasta in 1..totalPaginas && desde <= hasta) {
                                    viewModel.dividirPorRango(desde, hasta, nombreBase)
                                } else {
                                    Toast.makeText(context, "Rango inválido", Toast.LENGTH_SHORT).show()
                                }
                            }
                            1 -> {
                                val partes = partesIguales.toIntOrNull() ?: 2
                                if (partes >= 2) {
                                    viewModel.dividirEnPartes(partes, nombreBase)
                                } else {
                                    Toast.makeText(context, "Mínimo 2 partes", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = estado !is DividirPdfEstado.Procesando
                ) {
                    Text("Dividir y guardar")
                }
            }
        }
    }
}