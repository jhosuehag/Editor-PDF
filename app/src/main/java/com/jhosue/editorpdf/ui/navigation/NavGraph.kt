package com.jhosue.editorpdf.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.core.tween
import com.jhosue.editorpdf.ui.screens.DividirPdfScreen
import com.jhosue.editorpdf.ui.screens.EditorScreen
import com.jhosue.editorpdf.ui.screens.FirmaScreen
import com.jhosue.editorpdf.ui.screens.HomeScreen
import com.jhosue.editorpdf.ui.screens.ImagenesPdfScreen
import com.jhosue.editorpdf.ui.screens.SplashScreen
import com.jhosue.editorpdf.ui.screens.UnirPdfScreen

/**
 * Constantes de las rutas de navegación de PDFix
 */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val EDITOR = "editor"
    const val FIRMA = "firma"
    const val UNIR_PDF = "unir_pdf"
    const val DIVIDIR_PDF = "dividir_pdf"
    const val IMAGENES_PDF = "imagenes_pdf"
}

/**
 * Grafo de navegación de la aplicación
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { 300 }) },
        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -300 }) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) + slideInHorizontally(initialOffsetX = { -300 }) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { 300 }) }
    ) {
        // Pantalla Splash
        composable(Routes.SPLASH) {
            SplashScreen(onNavigateToHome = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        // Pantalla Principal (HOME)
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }

        // Editor
        composable(Routes.EDITOR) {
            EditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFirma = { navController.navigate(Routes.FIRMA) }
            )
        }

        // Firma
        composable(Routes.FIRMA) {
            FirmaScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }

        // Unir PDF
        composable(Routes.UNIR_PDF) {
            UnirPdfScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }

        // Dividir PDF
        composable(Routes.DIVIDIR_PDF) {
            DividirPdfScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }

        // Imágenes a PDF
        composable(Routes.IMAGENES_PDF) {
            ImagenesPdfScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }
    }
}

/**
 * Pantalla provisional (placeholder) para las rutas vacías
 */
@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
