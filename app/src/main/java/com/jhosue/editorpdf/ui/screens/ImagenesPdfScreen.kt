package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Pantalla para convertir una colección de imágenes en un archivo PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagenesPdfScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var orientacion by remember { mutableStateOf(0) }
    var margenesExpanded by remember { mutableStateOf(false) }
    var margenesSeleccionados by remember { mutableStateOf("Normales") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Imágenes a PDF", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            Text(
                text = "Imágenes seleccionadas (4)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(4) { index ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "IMG\n${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        IconButton(
                            onClick = { Toast.makeText(context, "Imagen eliminada", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Eliminar", 
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { Toast.makeText(context, "Seleccionar imágenes", Toast.LENGTH_SHORT).show() },
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
                                selected = orientacion == 0,
                                onClick = { orientacion = 0 },
                                label = { Text("Vertical") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = orientacion == 1,
                                onClick = { orientacion = 1 },
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
                            DropdownMenu(expanded = margenesExpanded, onDismissRequest = { margenesExpanded = false }) {
                                listOf("Sin márgenes", "Pequeños", "Normales").forEach { opcion ->
                                    DropdownMenuItem(
                                        text = { Text(opcion) },
                                        onClick = { 
                                            margenesSeleccionados = opcion
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
                onClick = { Toast.makeText(context, "Convirtiendo imágenes...", Toast.LENGTH_SHORT).show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Convertir a PDF")
            }
        }
    }
}
