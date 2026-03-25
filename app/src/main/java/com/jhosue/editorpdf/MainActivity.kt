package com.jhosue.editorpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.jhosue.editorpdf.ui.navigation.NavGraph
import com.jhosue.editorpdf.ui.theme.PDFixTheme

/**
 * Actividad principal de PDFix.
 * Punto de entrada de la aplicación.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilitar borde a borde para un aspecto más moderno
        enableEdgeToEdge()
        
        setContent {
            // Aplicar el tema personalizado de PDFix
            PDFixTheme {
                // Superficie base de la aplicación con el fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    
                    // Definir el NavGraph para la navegación general
                    NavGraph(navController = navController)
                }
            }
        }
    }
}