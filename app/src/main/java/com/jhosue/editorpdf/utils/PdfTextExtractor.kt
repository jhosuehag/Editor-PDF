package com.jhosue.editorpdf.utils

import android.graphics.RectF
import android.util.Log
import com.jhosue.editorpdf.data.models.TextBlock
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import java.io.File
import java.util.UUID

/**
 * Utilidad para extraer texto de documentos PDF usando iText7.
 * Proporciona métodos para extraer bloques de texto con sus posiciones
 * y propiedades de formato.
 */
class PdfTextExtractor {

    companion object {
        private const val TAG = "PdfTextExtractor"
    }

    /**
     * Resultado de la extracción de texto de una página.
     */
    data class ExtraccionResultado(
        val bloques: List<TextBlock>,
        val exito: Boolean,
        val mensajeError: String? = null
    )

    /**
     * Extrae los bloques de texto de una página específica del PDF.
     * Usa el método estático de PdfDocument para extraer texto.
     * @param pdfPath Ruta al archivo PDF.
     * @param pageIndex Índice de la página a extraer (0-based).
     * @return ExtraccionResultado con la lista de TextBlock o error.
     */
    fun extraerBloques(pdfPath: String, pageIndex: Int): ExtraccionResultado {
        return try {
            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                return ExtraccionResultado(
                    bloques = emptyList(),
                    exito = false,
                    mensajeError = "El archivo PDF no existe"
                )
            }

            val pdfReader = PdfReader(archivo)
            val pdfDoc = PdfDocument(pdfReader)
            
            // Verificar que la página existe
            val totalPaginas = pdfDoc.numberOfPages
            if (pageIndex < 0 || pageIndex >= totalPaginas) {
                pdfDoc.close()
                return ExtraccionResultado(
                    bloques = emptyList(),
                    exito = false,
                    mensajeError = "Página fuera de rango (1-$totalPaginas)"
                )
            }

            // iText usa 1-based page numbers
            val paginaPdf = pdfDoc.getPage(pageIndex + 1)
            val pageSize = paginaPdf.pageSize
            val pageHeight = pageSize.height.toInt()
            val pageWidth = pageSize.width.toInt()

            // Extraer texto usando el método estático de PdfDocument
            val textoCompleto = paginaPdf.document?.let { doc ->
                com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(paginaPdf)
            } ?: ""

            // Dividir el texto en líneas y crear bloques aproximados
            val lineas = textoCompleto.split("\n".toRegex()).filter { it.isNotBlank() }
            val bloques = mutableListOf<TextBlock>()
            
            var yActual = 50f
            val alturaLinea = 14f
            val margenIzquierdo = 50f
            
            for (linea in lineas) {
                if (linea.isBlank()) {
                    yActual += alturaLinea
                    continue
                }
                
                // Estimar ancho basado en longitud del texto y tamaño de fuente promedio
                val anchoEstimado = linea.length * 6f
                
                val rect = RectF(
                    margenIzquierdo,
                    (pageHeight - yActual - alturaLinea).coerceAtLeast(0f),
                    (margenIzquierdo + anchoEstimado).coerceAtMost(pageWidth.toFloat() - 50f),
                    (pageHeight - yActual).coerceAtMost(pageHeight.toFloat())
                )
                
                bloques.add(
                    TextBlock(
                        id = UUID.randomUUID().toString(),
                        texto = linea,
                        fuente = "Unknown",
                        tamanio = 12f,
                        negrita = false,
                        cursiva = false,
                        color = 0xFF000000.toInt(),
                        rect = rect,
                        pageIndex = pageIndex
                    )
                )
                
                yActual += alturaLinea + 4f
            }

            pdfDoc.close()

            Log.d(TAG, "Extraídos ${bloques.size} bloques de texto de página $pageIndex")
            ExtraccionResultado(bloques = bloques, exito = true)

        } catch (e: Exception) {
            Log.e(TAG, "Error al extraer texto de página $pageIndex", e)
            ExtraccionResultado(
                bloques = emptyList(),
                exito = false,
                mensajeError = "Error: ${e.message}"
            )
        }
    }
}
