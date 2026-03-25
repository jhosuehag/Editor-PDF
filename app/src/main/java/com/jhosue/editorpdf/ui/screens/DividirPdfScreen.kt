package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhosue.editorpdf.ui.components.RadioButtonRow

/**
 * Pantalla para extraer páginas o dividir un PDF por rangos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DividirPdfScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var modoDiv by remember { mutableStateOf(0) }
    var desdePagina by remember { mutableStateOf("1") }
    var hastaPagina by remember { mutableStateOf("6") }
    var partesIguales by remember { mutableStateOf("3") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dividir PDF", style = MaterialTheme.typography.titleMedium) },
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
                        Text("Informe_mensual.pdf", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("5.1 MB · 12 páginas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    TextButton(onClick = { Toast.makeText(context, "Módulo de selección", Toast.LENGTH_SHORT).show() }) {
                        Text("Cambiar")
                    }
                }
            }

            Text(
                "Selecciona páginas a dividir",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // GRILLA DE MINIATURAS (Simulada para selección)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(6) { index ->
                    val isChecked = index < 3
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8EEF7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✍", fontSize = 16.sp)
                            }
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall)
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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

            AnimatedVisibility(visible = modoDiv == 0, modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = desdePagina,
                        onValueChange = { desdePagina = it },
                        label = { Text("Desde") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(" al ", modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedTextField(
                        value = hastaPagina,
                        onValueChange = { hastaPagina = it },
                        label = { Text("Hasta") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            AnimatedVisibility(visible = modoDiv == 1, modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dividir en:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = partesIguales,
                        onValueChange = { partesIguales = it },
                        modifier = Modifier.width(70.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("partes iguales", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { Toast.makeText(context, "Dividiendo PDF...", Toast.LENGTH_SHORT).show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Dividir y guardar")
            }
        }
    }
}
