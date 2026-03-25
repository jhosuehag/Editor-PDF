package com.jhosue.editorpdf.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jhosue.editorpdf.ui.navigation.Routes

/**
 * Modelo de datos mock para los items de PDF
 */
data class PdfItemMock(
    val nombre: String,
    val fecha: String,
    val tamano: String
)

/**
 * Objeto con los datos mock solicitados
 */
object HomeScreenMock {
    val pdfs = listOf(
        PdfItemMock("Contrato_empresa.pdf", "Hoy, 10:32", "2.4 MB"),
        PdfItemMock("Informe_mensual.pdf", "Ayer, 18:15", "5.1 MB"),
        PdfItemMock("CV_actualizado.pdf", "20 mar", "890 KB"),
        PdfItemMock("Manual_usuario.pdf", "15 mar", "12.3 MB"),
        PdfItemMock("Presupuesto_2025.pdf", "10 mar", "1.7 MB"),
        PdfItemMock("Acta_reunion.pdf", "5 mar", "430 KB")
    )
}

/**
 * Pantalla Principal (HOME) de PDFix
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) } // Seteado a true para demostración del Shimmer
    val pdfs = HomeScreenMock.pdfs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PDFix",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Búsqueda...", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.History, contentDescription = "Recientes") },
                    label = { Text("Recientes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Build, contentDescription = "Herramientas") },
                    label = { Text("Herramientas") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { Toast.makeText(context, "Selecciona un PDF", Toast.LENGTH_SHORT).show() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        if (pdfs.isEmpty()) {
            EmptyStateView(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECCIÓN: Accesos rápidos
                item {
                    QuickAccessSection(onNavigate = onNavigate)
                }

                // SECCIÓN: Recientes (Header)
                item {
                    Text(
                        text = "Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // SECCIÓN: Recientes (Items)
                if (isLoading) {
                    items(3) {
                        com.jhosue.editorpdf.ui.components.ShimmerPdfItem()
                    }
                } else {
                    items(pdfs) { pdf ->
                        RecentPdfCard(
                            pdf = pdf,
                            onClick = { onNavigate(Routes.EDITOR) },
                            onMenuClick = { Toast.makeText(context, "Menú de ${pdf.nombre}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente de Accesos Rápidos (Unir, Dividir, Img a PDF)
 */
@Composable
fun QuickAccessSection(onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAccessCard(
            modifier = Modifier.weight(1f),
            title = "Unir PDFs",
            icon = Icons.Filled.MergeType,
            onClick = { onNavigate(Routes.UNIR_PDF) }
        )
        QuickAccessCard(
            modifier = Modifier.weight(1f),
            title = "Dividir PDF",
            icon = Icons.Filled.ContentCut,
            onClick = { onNavigate(Routes.DIVIDIR_PDF) }
        )
        QuickAccessCard(
            modifier = Modifier.weight(1f),
            title = "Img a PDF",
            icon = Icons.Filled.Image,
            onClick = { onNavigate(Routes.IMAGENES_PDF) }
        )
    }
}

/**
 * Tarjeta individual de acceso rápido
 */
@Composable
fun QuickAccessCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Tarjeta de un archivo PDF reciente
 */
@Composable
fun RecentPdfCard(
    pdf: PdfItemMock,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono PDF estilizado
            Box(
                modifier = Modifier
                    .size(48.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info del PDF
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${pdf.fecha} · ${pdf.tamano}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Menú contextual
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menú",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Vista mostrada cuando no hay archivos recientes
 */
@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha(0.3f),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin archivos recientes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Importa un PDF para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
