package com.jhosue.editorpdf.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Pantalla Splash que se muestra al iniciar la aplicación.
 * @param onNavigateToHome Callback que se ejecuta después de 2 segundos.
 */
@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    // Escuchar el efecto lanzado al cargar la pantalla
    LaunchedEffect(Unit) {
        delay(2000L) // Esperar 2 segundos
        onNavigateToHome()
    }

    // Configurar layout de la Splash
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícono SVG PDF (Dibujado vía Canvas)
            PdfLogo(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Nombre de la App
            Text(
                text = "PDFix",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtítulo
            Text(
                text = "Editor de PDF profesional",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Función que dibuja el logo de PDF solicitado (Rectángulo con doblez y texto "PDF")
 */
@Composable
fun PdfLogo(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val foldSize = width * 0.25f
        
        // Cuerpo del documento (Rectángulo con esquina doblada)
        // 1. Dibujar el cuerpo principal sin la esquina superior derecha
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(width - foldSize, 0f)
            lineTo(width, foldSize)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path = path, color = color)
        
        // 2. Dibujar el doblez de la esquina
        val foldPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(width - foldSize, 0f)
            lineTo(width - foldSize, foldSize)
            lineTo(width, foldSize)
            close()
        }
        drawPath(path = foldPath, color = color.copy(alpha = 0.8f))
        
        // El texto "PDF" se dibujará sobrepuesto conceptualmente para esta subfase
    }
}
