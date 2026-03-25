package com.jhosue.editorpdf.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.AffineTransform
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.ContentEdit
import com.jhosue.editorpdf.data.models.LayerData
import com.jhosue.editorpdf.data.models.PdfState
import com.jhosue.editorpdf.data.models.ShapeType
import com.jhosue.editorpdf.data.models.TextBlock
import com.jhosue.editorpdf.repository.LayerRepository
import com.jhosue.editorpdf.repository.PageRepository
import com.jhosue.editorpdf.repository.PdfRenderRepository
import com.jhosue.editorpdf.repository.SaveRepository
import com.jhosue.editorpdf.repository.SaveResult
import com.jhosue.editorpdf.utils.CoordinateMapper
import com.jhosue.editorpdf.utils.PdfImageExtractor
import com.jhosue.editorpdf.utils.PdfTextExtractor
import com.jhosue.editorpdf.utils.ShareUtils
import com.jhosue.editorpdf.utils.UndoRedoManager
import com.jhosue.editorpdf.utils.commands.AddAnnotationCommand
import com.jhosue.editorpdf.utils.commands.DeletePageCommand
import com.jhosue.editorpdf.utils.commands.RemoveAnnotationCommand
import com.jhosue.editorpdf.utils.commands.RotatePageCommand
import com.jhosue.editorpdf.utils.commands.TextEditCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * ViewModel para la pantalla de Editor de PDF.
 * Maneja el estado del PDF, paginación, renderizado y capas de anotaciones.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfRepository = PdfRenderRepository(application)
    private val layerRepository = LayerRepository(application)
    private val pageRepository = PageRepository()
    private val saveRepository = SaveRepository(application)
    private val textExtractor = PdfTextExtractor()
    private val imageExtractor = PdfImageExtractor()
    val undoRedoManager = UndoRedoManager()

    /**
     * Estado actual del PDF (Idle, Loading, Success, Error).
     */
    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Idle)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    /**
     * Índice de la página actualmente visualizada (0-based).
     */
    private val _paginaActual = MutableStateFlow(0)
    val paginaActual: StateFlow<Int> = _paginaActual.asStateFlow()

    /**
     * Número total de páginas del documento.
     */
    private val _totalPaginas = MutableStateFlow(0)
    val totalPaginas: StateFlow<Int> = _totalPaginas.asStateFlow()

    /**
     * Bitmap de la página actualmente renderizada.
     */
    private val _bitmapActual = MutableStateFlow<Bitmap?>(null)
    val bitmapActual: StateFlow<Bitmap?> = _bitmapActual.asStateFlow()

    /**
     * Nombre del archivo PDF actualmente abierto.
     */
    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    /**
     * URI del archivo PDF actualmente abierto.
     */
    private val _pdfUriActual = MutableStateFlow<Uri?>(null)
    val pdfUriActual: StateFlow<Uri?> = _pdfUriActual.asStateFlow()

    /**
     * Factor de zoom actual (1.0 = 100%).
     */
    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    /**
     * Offset vertical para scrolling.
     */
    private val _offsetY = MutableStateFlow(0f)
    val offsetY: StateFlow<Float> = _offsetY.asStateFlow()

    /**
     * Datos de la capa actual (anotaciones y ediciones de la página).
     */
    private val _layerActual = MutableStateFlow(LayerData(0))
    val layerActual: StateFlow<LayerData> = _layerActual.asStateFlow()

    /**
     * Ancho original de la página PDF en puntos.
     */
    private val _pdfPageWidth = MutableStateFlow(0)
    val pdfPageWidth: StateFlow<Int> = _pdfPageWidth.asStateFlow()

    /**
     * Alto original de la página PDF en puntos.
     */
    private val _pdfPageHeight = MutableStateFlow(0)
    val pdfPageHeight: StateFlow<Int> = _pdfPageHeight.asStateFlow()

    /**
     * Herramienta de anotación activa actualmente.
     * Valores posibles: null, "RESALTAR", "SUBRAYAR", "TACHAR", "DIBUJO", "TEXTO_LIBRE", "NOTA", "FORMA", "FIRMA"
     */
    private val _herramientaActivaAnotar = MutableStateFlow<String?>(null)
    val herramientaActivaAnotar: StateFlow<String?> = _herramientaActivaAnotar.asStateFlow()

    /**
     * Color actual para anotaciones (formato ARGB).
     */
    private val _colorAnotacionActual = MutableStateFlow(0xFFFFE066.toInt())
    val colorAnotacionActual: StateFlow<Int> = _colorAnotacionActual.asStateFlow()

    /**
     * Grosor actual para dibujo libre.
     */
    private val _grosorDibujo = MutableStateFlow(4f)
    val grosorDibujo: StateFlow<Float> = _grosorDibujo.asStateFlow()

    /**
     * Tipo de forma seleccionada para dibujar.
     */
    private val _formaSeleccionada = MutableStateFlow(ShapeType.RECTANGULO)
    val formaSeleccionada: StateFlow<ShapeType> = _formaSeleccionada.asStateFlow()

    /**
     * Lista de bloques de texto extraídos de la página actual.
     */
    private val _textBlocksActuales = MutableStateFlow<List<TextBlock>>(emptyList())
    val textBlocksActuales: StateFlow<List<TextBlock>> = _textBlocksActuales.asStateFlow()

    /**
     * Bloque de texto seleccionado actualmente (para resaltado).
     */
    private val _bloqueSeleccionado = MutableStateFlow<TextBlock?>(null)
    val bloqueSeleccionado: StateFlow<TextBlock?> = _bloqueSeleccionado.asStateFlow()

    /**
     * Indica si el modo de edición de contenido está activo.
     */
    private val _modoEdicionActivo = MutableStateFlow(false)
    val modoEdicionActivo: StateFlow<Boolean> = _modoEdicionActivo.asStateFlow()

    // === ESTADOS DE FORMATO DE TEXTO ===

    /**
     * Fuente actualmente seleccionada para edición de texto.
     */
    private val _fuenteSeleccionada = MutableStateFlow("Helvetica")
    val fuenteSeleccionada: StateFlow<String> = _fuenteSeleccionada.asStateFlow()

    /**
     * Tamaño de fuente seleccionado para edición de texto.
     */
    private val _tamanioSeleccionado = MutableStateFlow(12f)
    val tamanioSeleccionado: StateFlow<Float> = _tamanioSeleccionado.asStateFlow()

    /**
     * Indica si el texto editado será en negrita.
     */
    private val _esNegrita = MutableStateFlow(false)
    val esNegrita: StateFlow<Boolean> = _esNegrita.asStateFlow()

    /**
     * Indica si el texto editado será en cursiva.
     */
    private val _esCursiva = MutableStateFlow(false)
    val esCursiva: StateFlow<Boolean> = _esCursiva.asStateFlow()

    /**
     * Color del texto editado.
     */
    private val _colorTexto = MutableStateFlow(Color.Black)
    val colorTexto: StateFlow<Color> = _colorTexto.asStateFlow()

    /**
     * Alineación del texto: "LEFT", "CENTER", "RIGHT".
     */
    private val _alineacionTexto = MutableStateFlow("LEFT")
    val alineacionTexto: StateFlow<String> = _alineacionTexto.asStateFlow()

    // === ESTADOS DE IMÁGENES ===

    /**
     * Lista de imágenes extraídas de la página actual.
     */
    private val _imagenesActuales = MutableStateFlow<List<PdfImageExtractor.ImageBlock>>(emptyList())
    val imagenesActuales: StateFlow<List<PdfImageExtractor.ImageBlock>> = _imagenesActuales.asStateFlow()

    /**
     * Imagen seleccionada para reemplazo.
     */
    private val _imagenSeleccionada = MutableStateFlow<PdfImageExtractor.ImageBlock?>(null)
    val imagenSeleccionada: StateFlow<PdfImageExtractor.ImageBlock?> = _imagenSeleccionada.asStateFlow()

    /**
     * Bytes de la imagen nueva a insertar.
     */
    private val _imagenNuevaBytes = MutableStateFlow<ByteArray?>(null)
    val imagenNuevaBytes: StateFlow<ByteArray?> = _imagenNuevaBytes.asStateFlow()

    /**
     * Indica si estamos en modo de posicionar imagen para insertar.
     */
    private val _modoPosicionarImagen = MutableStateFlow(false)
    val modoPosicionarImagen: StateFlow<Boolean> = _modoPosicionarImagen.asStateFlow()

    /**
     * Abre un PDF desde la URI proporcionada.
     * Maneja cualquier excepción no controlada para evitar crashes.
     * @param uri URI del archivo PDF a abrir.
     */
    fun abrirPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _pdfState.value = PdfState.Loading
                _pdfUriActual.value = uri
                
                val resultado = pdfRepository.openPdf(uri)
                _pdfState.value = resultado

                if (resultado is PdfState.Success) {
                    _totalPaginas.value = resultado.totalPages
                    _fileName.value = resultado.fileName
                    _paginaActual.value = 0
                    cargarPagina(0)
                }
            } catch (e: Exception) {
                Log.e("EditorViewModel", "Error inesperado al abrir PDF", e)
                _pdfState.value = PdfState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    /**
     * Carga y renderiza una página específica.
     * También precarga las páginas adyacentes (N-1 y N+1) en background
     * para mejorar la fluidez de navegación.
     * 
     * @param index Índice de la página a cargar (0-based).
     */
    fun cargarPagina(index: Int) {
        if (index < 0 || index >= _totalPaginas.value) return

        viewModelScope.launch(Dispatchers.IO) {
            val resultado = pdfRepository.renderPage(index, targetWidth = 1080)
            resultado?.let {
                _bitmapActual.value = it.bitmap
                _pdfPageWidth.value = it.originalWidth
                _pdfPageHeight.value = it.originalHeight
            }
            _paginaActual.value = index
            // Reiniciar offset al cambiar de página
            _offsetY.value = 0f
            // Cargar la capa de la página
            cargarLayer(index)

            // OPTIMIZACIÓN: Precargar páginas adyacentes en background
            // Esto mejora la respuesta al navegar porque las páginas ya están renderizadas
            val paginaAnterior = index - 1
            val paginaSiguiente = index + 1
            if (paginaAnterior >= 0) {
                pdfRepository.precargarPagina(paginaAnterior, 1080)
            }
            if (paginaSiguiente < _totalPaginas.value) {
                pdfRepository.precargarPagina(paginaSiguiente, 1080)
            }
        }
    }

    /**
     * Carga la capa de una página desde Room.
     * @param pageIndex Índice de la página.
     */
    private suspend fun cargarLayer(pageIndex: Int) {
        val layer = layerRepository.obtenerLayer(pageIndex)
        if (layer != null) {
            _layerActual.value = layer
        } else {
            // Crear capa vacía para la página
            _layerActual.value = LayerData(pageIndex)
        }
    }

    /**
     * Carga la página siguiente.
     */
    fun siguientePagina() {
        val siguiente = _paginaActual.value + 1
        if (siguiente < _totalPaginas.value) {
            cargarPagina(siguiente)
        }
    }

    /**
     * Carga la página anterior.
     */
    fun anteriorPagina() {
        val anterior = _paginaActual.value - 1
        if (anterior >= 0) {
            cargarPagina(anterior)
        }
    }

    /**
     * Actualiza el nivel de zoom.
     * @param scale Nuevo factor de zoom.
     */
    fun setZoomLevel(scale: Float) {
        _zoomLevel.value = scale.coerceIn(0.5f, 3f)
    }

    /**
     * Actualiza el offset vertical para scroll.
     * @param offset Nuevo offset en el eje Y.
     */
    fun setOffsetY(offset: Float) {
        _offsetY.value = offset
    }

    /**
     * Agrega zoom al nivel actual.
     * @param delta Cantidad a agregar al zoom actual.
     */
    fun agregarZoom(delta: Float) {
        val nuevoZoom = _zoomLevel.value + delta
        _zoomLevel.value = nuevoZoom.coerceIn(0.5f, 3f)
    }

    /**
     * Cierra el PDF y libera recursos.
     */
    fun cerrarPdf() {
        pdfRepository.closePdf()
        undoRedoManager.limpiar()
        _pdfState.value = PdfState.Idle
        _paginaActual.value = 0
        _totalPaginas.value = 0
        _bitmapActual.value = null
        _fileName.value = ""
        _pdfUriActual.value = null
        _zoomLevel.value = 1f
        _offsetY.value = 0f
        _layerActual.value = LayerData(0)
        _pdfPageWidth.value = 0
        _pdfPageHeight.value = 0
        _herramientaActivaAnotar.value = null
        _textBlocksActuales.value = emptyList()
        _bloqueSeleccionado.value = null
        _modoEdicionActivo.value = false
    }

    /**
     * Establece la herramienta de anotación activa.
     * @param herramienta Nombre de la herramienta o null para desactivar.
     */
    fun setHerramientaAnotacion(herramienta: String?) {
        _herramientaActivaAnotar.value = herramienta
    }

    /**
     * Establece el color de anotación actual.
     * @param color Color en formato ARGB.
     */
    fun setColorAnotacion(color: Int) {
        _colorAnotacionActual.value = color
    }

    /**
     * Establece el grosor del dibujo.
     * @param grosor Grosor en píxeles.
     */
    fun setGrosorDibujo(grosor: Float) {
        _grosorDibujo.value = grosor
    }

    /**
     * Establece el tipo de forma seleccionada.
     * @param forma Tipo de forma geométrica.
     */
    fun setFormaSeleccionada(forma: ShapeType) {
        _formaSeleccionada.value = forma
    }

    /**
     * Activa o desactiva el modo de edición de contenido.
     * @param activo true para activar, false para desactivar.
     */
    fun setModoEdicionActivo(activo: Boolean) {
        _modoEdicionActivo.value = activo
        if (!activo) {
            // Limpiar selección al desactivar
            _bloqueSeleccionado.value = null
            _textBlocksActuales.value = emptyList()
        }
    }

    /**
     * Carga los bloques de texto de la página actual.
     * Extrae el texto usando PdfTextExtractor y actualiza el estado.
     */
    fun cargarTextBlocks() {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfPath = pdfRepository.getPdfFilePath()
            if (pdfPath != null) {
                val resultado = textExtractor.extraerBloques(pdfPath, _paginaActual.value)
                if (resultado.exito) {
                    _textBlocksActuales.value = resultado.bloques
                } else {
                    _textBlocksActuales.value = emptyList()
                }
            }
        }
    }

    /**
     * Selecciona un bloque de texto para resaltado.
     * @param bloque Bloque a seleccionar o null para deseleccionar.
     */
    fun seleccionarBloque(bloque: TextBlock?) {
        _bloqueSeleccionado.value = bloque
    }

    /**
     * Busca el bloque de texto que contiene el punto especificado.
     * @param screenPoint Punto en coordenadas de pantalla.
     * @param screenWidth Ancho de la vista de pantalla.
     * @param screenHeight Alto de la vista de pantalla.
     * @return TextBlock que contiene el punto o null si no hay ninguno.
     */
    fun buscarBloqueEnPunto(
        screenPoint: Offset,
        screenWidth: Float,
        screenHeight: Float
    ): TextBlock? {
        val pdfPoint = CoordinateMapper.screenToPdf(
            screenPoint = screenPoint,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            pdfPageWidth = _pdfPageWidth.value,
            pdfPageHeight = _pdfPageHeight.value,
            zoom = _zoomLevel.value,
            offsetX = 0f,
            offsetY = _offsetY.value
        )

        return _textBlocksActuales.value.find { bloque ->
            bloque.rect.contains(pdfPoint.x, pdfPoint.y)
        }
    }

    /**
     * Procesa un toque en la pantalla según la herramienta activa.
     * Crea y agrega la anotación correspondiente.
     * @param screenPoint Punto tocado en coordenadas de pantalla.
     * @param screenWidth Ancho de la vista de pantalla.
     * @param screenHeight Alto de la vista de pantalla.
     */
    fun procesarToqueAnotacion(
        screenPoint: Offset,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val herramienta = _herramientaActivaAnotar.value ?: return
        
        // Convertir a coordenadas PDF
        val pdfPoint = CoordinateMapper.screenToPdf(
            screenPoint = screenPoint,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            pdfPageWidth = _pdfPageWidth.value,
            pdfPageHeight = _pdfPageHeight.value,
            zoom = _zoomLevel.value,
            offsetX = 0f,
            offsetY = _offsetY.value
        )

        when (herramienta) {
            "RESALTAR" -> crearHighlight(pdfPoint)
            "SUBRAYAR" -> crearUnderline(pdfPoint)
            "TACHAR" -> crearStrikethrough(pdfPoint)
        }
    }

    /**
     * Crea un highlight en la posición especificada.
     * Como no tenemos detección de texto real, usamos un rectángulo genérico.
     */
    private fun crearHighlight(pdfPoint: PointF) {
        val rect = crearRectanguloGenerico(pdfPoint)
        val highlight = AnnotationElement.Highlight(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            rects = listOf(rect),
            color = _colorAnotacionActual.value
        )
        agregarAnotacion(highlight)
    }

    private fun crearUnderline(pdfPoint: PointF) {
        val rect = crearRectanguloGenerico(pdfPoint)
        val underline = AnnotationElement.Underline(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            rects = listOf(rect),
            color = _colorAnotacionActual.value
        )
        agregarAnotacion(underline)
    }

    private fun crearStrikethrough(pdfPoint: PointF) {
        val rect = crearRectanguloGenerico(pdfPoint)
        val strikethrough = AnnotationElement.Strikethrough(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            rects = listOf(rect),
            color = _colorAnotacionActual.value
        )
        agregarAnotacion(strikethrough)
    }

    /**
     * Crea un rectángulo genérico alrededor de un punto.
     * Usado cuando no hay detección de texto real.
     */
    private fun crearRectanguloGenerico(pdfPoint: PointF): RectF {
        val ancho = 100f
        val alto = 20f
        return RectF(
            pdfPoint.x - ancho / 2,
            pdfPoint.y - alto / 2,
            pdfPoint.x + ancho / 2,
            pdfPoint.y + alto / 2
        )
    }

    /**
     * Agrega una anotación de dibujo libre.
     * @param puntos Lista de puntos del trazo en coordenadas PDF.
     */
    fun agregarDibujoLibre(puntos: List<PointF>) {
        if (puntos.size < 2) return
        val freeDrawing = AnnotationElement.FreeDrawing(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            puntos = puntos,
            color = _colorAnotacionActual.value,
            grosor = _grosorDibujo.value
        )
        agregarAnotacion(freeDrawing)
    }

    /**
     * Agrega una anotación de forma geométrica.
     * @param puntoInicio Punto de inicio en coordenadas PDF.
     * @param puntoFin Punto de fin en coordenadas PDF.
     */
    fun agregarForma(puntoInicio: PointF, puntoFin: PointF) {
        val shape = AnnotationElement.Shape(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            tipo = _formaSeleccionada.value,
            puntoInicio = puntoInicio,
            puntoFin = puntoFin,
            color = _colorAnotacionActual.value,
            grosor = _grosorDibujo.value
        )
        agregarAnotacion(shape)
    }

    /**
     * Agrega texto libre en una posición.
     * @param pdfPoint Posición en coordenadas PDF.
     * @param texto Texto a mostrar.
     */
    fun agregarTextoLibre(pdfPoint: PointF, texto: String) {
        val freeText = AnnotationElement.FreeText(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            texto = texto,
            posicion = pdfPoint,
            color = _colorAnotacionActual.value,
            tamanio = 16f
        )
        agregarAnotacion(freeText)
    }

    /**
     * Agrega una nota adhesiva en una posición.
     * @param pdfPoint Posición en coordenadas PDF.
     * @param texto Contenido de la nota.
     */
    fun agregarNotaAdhesiva(pdfPoint: PointF, texto: String) {
        val stickyNote = AnnotationElement.StickyNote(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            texto = texto,
            posicion = pdfPoint,
            estaColapsada = false
        )
        agregarAnotacion(stickyNote)
    }

    /**
     * Agrega una anotación a la capa actual usando el sistema de comandos.
     * @param anotacion Anotación a agregar.
     */
    fun agregarAnotacion(anotacion: AnnotationElement) {
        viewModelScope.launch(Dispatchers.IO) {
            val comando = AddAnnotationCommand(
                anotacion = anotacion,
                layerRepository = layerRepository,
                layerData = _layerActual
            )
            undoRedoManager.ejecutar(comando)
        }
    }

    /**
     * Elimina una anotación específica usando el sistema de comandos.
     * @param anotacion Anotación a eliminar.
     */
    fun eliminarAnotacion(anotacion: AnnotationElement) {
        viewModelScope.launch(Dispatchers.IO) {
            val comando = RemoveAnnotationCommand(
                anotacion = anotacion,
                layerRepository = layerRepository,
                layerData = _layerActual
            )
            undoRedoManager.ejecutar(comando)
        }
    }

    /**
     * Limpia todas las anotaciones de la página actual.
     */
    fun limpiarAnotaciones() {
        viewModelScope.launch(Dispatchers.IO) {
            val layerActual = _layerActual.value
            // Crear comandos de eliminación para cada anotación
            for (anotacion in layerActual.anotaciones) {
                val comando = RemoveAnnotationCommand(
                    anotacion = anotacion,
                    layerRepository = layerRepository,
                    layerData = _layerActual
                )
                undoRedoManager.ejecutar(comando)
            }
        }
    }

    /**
     * Agrega una firma en una posición específica.
     * @param bitmapBytes Bytes de la imagen PNG de la firma.
     * @param pdfPoint Posición en coordenadas PDF donde colocar la firma.
     * @param escala Factor de escala de la firma.
     */
    fun agregarFirma(bitmapBytes: ByteArray, pdfPoint: PointF, escala: Float) {
        val firma = AnnotationElement.Signature(
            id = UUID.randomUUID().toString(),
            pageIndex = _paginaActual.value,
            bitmapBytes = bitmapBytes,
            posicion = pdfPoint,
            escala = escala
        )
        agregarAnotacion(firma)
    }

    /**
     * Agrega una firma desde una entidad de firma existente.
     * @param firmaBitmap Bitmap de la firma.
     * @param pdfPoint Posición en coordenadas PDF donde colocar la firma.
     * @param escala Factor de escala de la firma.
     */
    fun agregarFirmaDesdeBitmap(firmaBitmap: Bitmap, pdfPoint: PointF, escala: Float) {
        val outputStream = java.io.ByteArrayOutputStream()
        firmaBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        agregarFirma(bytes, pdfPoint, escala)
    }

    /**
     * Edita el texto de un bloque seleccionado.
     * Aplica el cambio al PDF usando iText7 y actualiza el estado.
     * @param bloque Bloque de texto a editar.
     * @param textoNuevo Nuevo texto que reemplazará al original.
     */
    fun editarTextoBloque(bloque: TextBlock, textoNuevo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfPath = pdfRepository.getPdfFilePath() ?: return@launch
            
            // 1. Crear ContentEdit.TextEdit con los datos del bloque y formato actual
            val edit = ContentEdit.TextEdit(
                id = UUID.randomUUID().toString(),
                pageIndex = bloque.pageIndex,
                textoOriginal = bloque.texto,
                textoNuevo = textoNuevo,
                posicionRect = bloque.rect,
                fuente = _fuenteSeleccionada.value,
                tamanio = _tamanioSeleccionado.value,
                color = android.graphics.Color.argb(
                    255,
                    (_colorTexto.value.red * 255).toInt(),
                    (_colorTexto.value.green * 255).toInt(),
                    (_colorTexto.value.blue * 255).toInt()
                ),
                negrita = _esNegrita.value,
                cursiva = _esCursiva.value,
                alineacion = _alineacionTexto.value
            )

            // 2. Guardarlo en LayerData
            val layerActual = _layerActual.value
            _layerActual.value = layerActual.copy(
                ediciones = layerActual.ediciones + edit
            )
            layerRepository.guardarLayer(_layerActual.value)

            // 3. Crear y ejecutar comando de edición de texto
            val comando = TextEditCommand(
                edit = edit,
                layerRepository = layerRepository,
                layerData = _layerActual,
                pdfPath = pdfPath
            )
            undoRedoManager.ejecutar(comando)

            // 4. Re-renderizar la página actual
            cargarPagina(_paginaActual.value)

            // 5. Recargar los text blocks actualizados
            cargarTextBlocks()

            // 6. Limpiar selección
            _bloqueSeleccionado.value = null
        }
    }

    // === MÉTODOS DE FORMATO DE TEXTO ===

    /**
     * Establece la fuente seleccionada para edición de texto.
     * @param fuente Nombre de la fuente.
     */
    fun setFuenteSeleccionada(fuente: String) {
        _fuenteSeleccionada.value = fuente
    }

    /**
     * Establece el tamaño de fuente para edición de texto.
     * @param tamanio Tamaño en puntos.
     */
    fun setTamanioSeleccionado(tamanio: Float) {
        _tamanioSeleccionado.value = tamanio
    }

    /**
     * Activa o desactiva el formato negrita.
     * @param negrita true para activar negrita.
     */
    fun setEsNegrita(negrita: Boolean) {
        _esNegrita.value = negrita
    }

    /**
     * Activa o desactiva el formato cursiva.
     * @param cursiva true para activar cursiva.
     */
    fun setEsCursiva(cursiva: Boolean) {
        _esCursiva.value = cursiva
    }

    /**
     * Establece el color del texto.
     * @param color Color de Compose.
     */
    fun setColorTexto(color: Color) {
        _colorTexto.value = color
    }

    /**
     * Establece la alineación del texto.
     * @param alineacion "LEFT", "CENTER" o "RIGHT".
     */
    fun setAlineacionTexto(alineacion: String) {
        _alineacionTexto.value = alineacion
    }

    /**
     * Aplica una edición de texto al archivo PDF usando iText7.
     * Cubre el texto original con un rectángulo blanco y escribe el texto nuevo.
     * @param edit Edición de texto a aplicar.
     * @return true si tuvo éxito, false si falló.
     */
    private suspend fun aplicarTextEditEnPdf(edit: ContentEdit.TextEdit): Boolean {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return false
        
        return try {
            val archivo = File(pdfPath)
            if (!archivo.exists()) {
                Log.e("EditorViewModel", "Archivo PDF no encontrado: $pdfPath")
                return false
            }

            // Crear respaldo
            val archivoBackup = File("$pdfPath.backup")
            archivo.copyTo(archivoBackup, overwrite = true)
            Log.d("EditorViewModel", "Backup creado en: ${archivoBackup.absolutePath}")

            // Abrir el documento para lectura/escritura
            val pdfReader = PdfReader(archivo)
            val pdfWriter = PdfWriter(archivo)
            val pdfDoc = PdfDocument(pdfReader, pdfWriter)

            // Obtener la página (iText usa 1-based)
            val pagina: PdfPage = pdfDoc.getPage(edit.pageIndex + 1)
            val pageHeight = pagina.pageSize.height

            // Crear canvas para dibujar sobre la página
            val canvas = PdfCanvas(pagina)

            // Obtener posición en coordenadas iText (origen abajo-izquierda)
            val x = edit.posicionRect.left
            // Convertir Y de coordenadas Android (arriba-izquierda) a iText (abajo-izquierda)
            val y = pageHeight - edit.posicionRect.bottom

            // Dibujar rectángulo blanco sobre el texto original para ocultarlo
            canvas.setFillColor(DeviceRgb(255, 255, 255))
            canvas.rectangle(
                x.toDouble(),
                y.toDouble(),
                edit.posicionRect.width().toDouble(),
                edit.posicionRect.height().toDouble()
            )
            canvas.fill()

            // Crear fuente para el texto nuevo según formato
            val font = crearFuente(edit.fuente, edit.negrita, edit.cursiva)

            // Convertir color ARGB a iText color
            val alpha = (edit.color shr 24) and 0xFF
            val red = (edit.color shr 16) and 0xFF
            val green = (edit.color shr 8) and 0xFF
            val blue = edit.color and 0xFF
            val textColor = DeviceRgb(red, green, blue)

            // Establecer color y fuente para el texto nuevo
            canvas.setFontAndSize(font, edit.tamanio)
            canvas.setFillColor(textColor)

            // Escribir el texto nuevo en la posición con alineación
            val lineas = edit.textoNuevo.split("\n")
            var yActual = y
            val alturaLinea = edit.tamanio * 1.2f

            for (linea in lineas) {
                // Calcular posición X según alineación
                val xAlineado = calcularXAlineado(linea, edit.posicionRect, edit.tamanio, edit.alineacion)
                
                canvas.beginText()
                canvas.moveText(xAlineado.toDouble(), yActual.toDouble())
                canvas.showText(linea)
                canvas.endText()
                yActual -= alturaLinea
            }

            // Cerrar el documento
            pdfDoc.close()

            Log.d("EditorViewModel", "Texto modificado en página ${edit.pageIndex}")
            true
        } catch (e: Exception) {
            Log.e("EditorViewModel", "Error al modificar texto en PDF", e)
            // Restaurar del backup si hubo error
            val archivo = File(pdfPath)
            val backup = File("$pdfPath.backup")
            if (backup.exists()) {
                backup.copyTo(archivo, overwrite = true)
                backup.delete()
            }
            false
        }
    }

    /**
     * Crea una fuente PDF según el nombre, negrita y cursiva.
     * @param fuenteNombre Nombre de la fuente base.
     * @param negrita Si debe ser negrita.
     * @param cursiva Si debe ser cursiva.
     * @return PdfFont creado.
     */
    private fun crearFuente(fuenteNombre: String, negrita: Boolean, cursiva: Boolean): com.itextpdf.kernel.font.PdfFont {
        val fuenteBase = when {
            fuenteNombre.contains("Times", ignoreCase = true) -> "Times"
            fuenteNombre.contains("Courier", ignoreCase = true) -> "Courier"
            else -> "Helvetica"
        }

        val sufijo = when {
            negrita && cursiva -> "BoldOblique"
            negrita -> "Bold"
            cursiva -> "Oblique"
            else -> ""
        }

        val nombreCompleto = "$fuenteBase$sufijo"
        return try {
            PdfFontFactory.createFont(nombreCompleto)
        } catch (e: Exception) {
            try {
                PdfFontFactory.createFont(fuenteBase)
            } catch (e2: Exception) {
                PdfFontFactory.createFont("Helvetica")
            }
        }
    }

    /**
     * Calcula la posición X para la alineación del texto.
     * @param texto Texto a escribir.
     * @param rect Rectángulo del texto original.
     * @param tamanio Tamaño de fuente.
     * @param alineacion Alineación: "LEFT", "CENTER", "RIGHT".
     * @return Posición X calculada.
     */
    private fun calcularXAlineado(texto: String, rect: RectF, tamanio: Float, alineacion: String): Float {
        // Estimación simple del ancho del texto (ancho promedio por carácter)
        val anchoTexto = texto.length * tamanio * 0.6f
        val anchoDisponible = rect.width()

        return when (alineacion.uppercase()) {
            "CENTER" -> rect.left + (anchoDisponible - anchoTexto) / 2
            "RIGHT" -> rect.right - anchoTexto
            else -> rect.left
        }.coerceAtLeast(rect.left)
    }

    // === MÉTODOS DE MANEJO DE IMÁGENES ===

    /**
     * Carga las imágenes de la página actual.
     */
    fun cargarImagenes() {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfPath = pdfRepository.getPdfFilePath()
            if (pdfPath != null) {
                val resultado = imageExtractor.extraerImagenes(pdfPath, _paginaActual.value)
                if (resultado.exito) {
                    _imagenesActuales.value = resultado.imagenes
                } else {
                    _imagenesActuales.value = emptyList()
                }
            }
        }
    }

    /**
     * Busca la imagen que contiene el punto especificado.
     * @param screenPoint Punto en coordenadas de pantalla.
     * @param screenWidth Ancho de la vista de pantalla.
     * @param screenHeight Alto de la vista de pantalla.
     * @return ImageBlock que contiene el punto o null.
     */
    fun buscarImagenEnPunto(
        screenPoint: Offset,
        screenWidth: Float,
        screenHeight: Float
    ): PdfImageExtractor.ImageBlock? {
        val pdfPoint = CoordinateMapper.screenToPdf(
            screenPoint = screenPoint,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            pdfPageWidth = _pdfPageWidth.value,
            pdfPageHeight = _pdfPageHeight.value,
            zoom = _zoomLevel.value,
            offsetX = 0f,
            offsetY = _offsetY.value
        )

        return _imagenesActuales.value.find { imagen ->
            imagen.rect.contains(pdfPoint.x, pdfPoint.y)
        }
    }

    /**
     * Reemplaza una imagen en el PDF con una nueva imagen.
     * Nota: La implementación real de reemplazo de imágenes en PDF es compleja
     * y requiere manipular el flujo de contenido. Esta versión registra la edición.
     * @param imagenOriginal Imagen a reemplazar.
     * @param nuevaImagenBytes Bytes de la nueva imagen.
     */
    fun reemplazarImagen(imagenOriginal: PdfImageExtractor.ImageBlock, nuevaImagenBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val edit = ContentEdit.ImageReplace(
                id = UUID.randomUUID().toString(),
                pageIndex = imagenOriginal.pageIndex,
                posicionRect = imagenOriginal.rect,
                nuevaImagenBytes = nuevaImagenBytes
            )

            // Guardar en LayerData
            val layerActual = _layerActual.value
            _layerActual.value = layerActual.copy(
                ediciones = layerActual.ediciones + edit
            )
            layerRepository.guardarLayer(_layerActual.value)

            // Re-renderizar para mostrar los cambios
            cargarPagina(_paginaActual.value)
            cargarImagenes()

            _imagenSeleccionada.value = null
            Log.d("EditorViewModel", "Imagen marcada para reemplazo en página ${edit.pageIndex}")
        }
    }

    /**
     * Inserta una imagen en una posición específica del PDF.
     * Nota: La implementación real de inserción de imágenes en PDF es compleja.
     * Esta versión registra la edición para aplicación futura.
     * @param posicion Posición en coordenadas PDF.
     * @param ancho Ancho de la imagen.
     * @param alto Alto de la imagen.
     * @param imagenBytes Bytes de la imagen.
     */
    fun insertarImagen(posicion: PointF, ancho: Float, alto: Float, imagenBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val edit = ContentEdit.ImageInsert(
                id = UUID.randomUUID().toString(),
                pageIndex = _paginaActual.value,
                posicion = posicion,
                imagenBytes = imagenBytes,
                ancho = ancho,
                alto = alto
            )

            // Guardar en LayerData
            val layerActual = _layerActual.value
            _layerActual.value = layerActual.copy(
                ediciones = layerActual.ediciones + edit
            )
            layerRepository.guardarLayer(_layerActual.value)

            // Re-renderizar
            cargarPagina(_paginaActual.value)
            cargarImagenes()

            _modoPosicionarImagen.value = false
            _imagenNuevaBytes.value = null
            Log.d("EditorViewModel", "Imagen marcada para inserción en página ${edit.pageIndex}")
        }
    }

    /**
     * Activa el modo de posicionar imagen para inserción.
     * @param imagenBytes Bytes de la imagen a insertar.
     */
    fun activarModoInsertarImagen(imagenBytes: ByteArray) {
        _imagenNuevaBytes.value = imagenBytes
        _modoPosicionarImagen.value = true
    }

    /**
     * Cancela el modo de posicionar imagen.
     */
    fun cancelarModoInsertarImagen() {
        _modoPosicionarImagen.value = false
        _imagenNuevaBytes.value = null
    }

    // === MÉTODOS DE GESTIÓN DE PÁGINAS ===

    /**
     * Estado para operaciones de página (loading, success, error).
     */
    private val _pageOperationState = MutableStateFlow<PageOperationState>(PageOperationState.Idle)
    val pageOperationState: StateFlow<PageOperationState> = _pageOperationState.asStateFlow()

    /**
     * Elimina una página del PDF usando el sistema de comandos.
     * @param pageIndex Índice de la página a eliminar (0-based).
     */
    fun eliminarPagina(pageIndex: Int) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Eliminando página...")
            
            val comando = DeletePageCommand(
                pageIndex = pageIndex,
                pageRepository = pageRepository,
                pdfPath = pdfPath,
                totalPaginasBefore = _totalPaginas.value
            )
            undoRedoManager.ejecutar(comando)
            
            // Actualizar el número total de páginas
            _totalPaginas.value = _totalPaginas.value - 1
            // Si la página eliminada era la actual o anterior, ajustar
            if (_paginaActual.value >= _totalPaginas.value && _totalPaginas.value > 0) {
                cargarPagina(_totalPaginas.value - 1)
            } else if (_totalPaginas.value > 0) {
                cargarPagina(_paginaActual.value)
            }
            
            _pageOperationState.value = PageOperationState.Success("Página eliminada")
        }
    }

    /**
     * Duplica una página del PDF.
     * @param pageIndex Índice de la página a duplicar (0-based).
     */
    fun duplicarPagina(pageIndex: Int) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Duplicando página...")
            
            val exito = pageRepository.duplicarPagina(pdfPath, pageIndex)
            
            if (exito) {
                _pageOperationState.value = PageOperationState.Success("Página duplicada")
                // Actualizar el número total de páginas
                _totalPaginas.value = _totalPaginas.value + 1
                // Recargar la página actual
                cargarPagina(_paginaActual.value)
            } else {
                _pageOperationState.value = PageOperationState.Error("Error al duplicar página")
            }
        }
    }

    /**
     * Inserta una página en blanco después de la página especificada.
     * @param pageIndex Índice donde insertar la nueva página (0-based).
     */
    fun insertarPaginaBlanco(pageIndex: Int) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Insertando página...")
            
            val exito = pageRepository.insertarPaginaBlanco(pdfPath, pageIndex)
            
            if (exito) {
                _pageOperationState.value = PageOperationState.Success("Página en blanco insertada")
                // Actualizar el número total de páginas
                _totalPaginas.value = _totalPaginas.value + 1
                // Navegar a la nueva página
                cargarPagina(pageIndex + 1)
            } else {
                _pageOperationState.value = PageOperationState.Error("Error al insertar página")
            }
        }
    }

    /**
     * Rota una página del PDF usando el sistema de comandos.
     * @param pageIndex Índice de la página a rotar (0-based).
     * @param grados Grados de rotación (90, 180, 270).
     */
    fun rotarPagina(pageIndex: Int, grados: Int = 90) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Rotando página...")
            
            val comando = RotatePageCommand(
                pageIndex = pageIndex,
                grados = grados,
                pageRepository = pageRepository,
                pdfPath = pdfPath
            )
            undoRedoManager.ejecutar(comando)
            
            // Recargar la página para mostrar la rotación
            cargarPagina(pageIndex)
            _pageOperationState.value = PageOperationState.Success("Página rotada")
        }
    }

    /**
     * Reordena las páginas del PDF.
     * @param nuevoOrden Lista de índices (0-based) con el nuevo orden.
     */
    fun reordenarPaginas(nuevoOrden: List<Int>) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Reordenando páginas...")
            
            val exito = pageRepository.reordenarPaginas(pdfPath, nuevoOrden)
            
            if (exito) {
                _pageOperationState.value = PageOperationState.Success("Páginas reordenadas")
                // Recargar la página actual
                if (_paginaActual.value < _totalPaginas.value) {
                    cargarPagina(_paginaActual.value)
                }
            } else {
                _pageOperationState.value = PageOperationState.Error("Error al reordenar páginas")
            }
        }
    }

    /**
     * Extrae una página como un nuevo PDF.
     * @param pageIndex Índice de la página a extraer (0-based).
     * @param destinoPath Ruta de destino para el nuevo PDF.
     */
    fun extraerPagina(pageIndex: Int, destinoPath: String) {
        val pdfPath = pdfRepository.getPdfFilePath() ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            _pageOperationState.value = PageOperationState.Loading("Extrayendo página...")
            
            val exito = pageRepository.extraerPagina(pdfPath, pageIndex, destinoPath)
            
            if (exito) {
                _pageOperationState.value = PageOperationState.Success("Página guardada como: ${File(destinoPath).name}")
            } else {
                _pageOperationState.value = PageOperationState.Error("Error al extraer página")
            }
        }
    }

    /**
     * Restablece el estado de operación de página a Idle.
     */
    fun resetPageOperationState() {
        _pageOperationState.value = PageOperationState.Idle
    }

    /**
     * Obtiene un bitmap de miniatura para una página específica.
     * @param pageIndex Índice de la página (0-based).
     * @param targetWidth Ancho objetivo para la miniatura.
     * @return Bitmap de la página o null si falla.
     */
    fun obtenerMiniaturaPagina(pageIndex: Int, targetWidth: Int = 200): Bitmap? {
        return null
    }

    /**
     * Deshace el último comando ejecutado.
     */
    fun deshacer() {
        viewModelScope.launch(Dispatchers.IO) {
            undoRedoManager.deshacer()
            // Recargar la página actual para mostrar los cambios
            cargarPagina(_paginaActual.value)
        }
    }

    /**
     * Rehace el último comando deshecho.
     */
    fun rehacer() {
        viewModelScope.launch(Dispatchers.IO) {
            undoRedoManager.rehacer()
            // Recargar la página actual para mostrar los cambios
            cargarPagina(_paginaActual.value)
        }
    }

    // === MÉTODOS DE GUARDADO ===

    /**
     * Estado actual de la operación de guardado.
     */
    private val _guardarEstado = MutableStateFlow<GuardarEstado>(GuardarEstado.Idle)
    val guardarEstado: StateFlow<GuardarEstado> = _guardarEstado.asStateFlow()

    /**
     * Guarda el PDF aplicando todas las anotaciones y ediciones.
     * Sobreescribe el archivo original.
     */
    fun guardar() {
        val pdfPath = pdfRepository.getPdfFilePath()
        if (pdfPath == null) {
            _guardarEstado.value = GuardarEstado.Error("No hay archivo PDF abierto")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _guardarEstado.value = GuardarEstado.Guardando("Guardando PDF...")

            // Obtener todas las capas guardadas
            val layers = layerRepository.obtenerTodasLasCapas()

            val resultado = saveRepository.guardarSuspend(pdfPath, layers)

            when (resultado) {
                is SaveResult.Exito -> {
                    _guardarEstado.value = GuardarEstado.Exito
                    undoRedoManager.limpiar()
                    // Limpiar backups temporales
                    limpiarBackups(pdfPath)
                }
                is SaveResult.Error -> {
                    _guardarEstado.value = GuardarEstado.Error(resultado.mensaje)
                }
            }
        }
    }

    /**
     * Guarda el PDF como un nuevo archivo.
     * @param nombre Nombre del nuevo archivo (sin extensión).
     */
    fun guardarComo(nombre: String) {
        val pdfPath = pdfRepository.getPdfFilePath()
        if (pdfPath == null) {
            _guardarEstado.value = GuardarEstado.Error("No hay archivo PDF abierto")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _guardarEstado.value = GuardarEstado.Guardando("Guardando copia...")

            // Obtener todas las capas guardadas
            val layers = layerRepository.obtenerTodasLasCapas()

            // Crear ruta de destino en el directorio de archivos externos
            val directorio = getApplication<Application>().getExternalFilesDir(null)
            val destinoPath = "${directorio?.absolutePath}/$nombre.pdf"

            val resultado = saveRepository.guardarComo(pdfPath, destinoPath, layers)

            when (resultado) {
                is SaveResult.Exito -> {
                    _guardarEstado.value = GuardarEstado.ExitoGuardarComo(destinoPath)
                    undoRedoManager.limpiar()
                }
                is SaveResult.Error -> {
                    _guardarEstado.value = GuardarEstado.Error(resultado.mensaje)
                }
            }
        }
    }

    /**
     * Restablece el estado de guardado a Idle.
     */
    fun resetearGuardarEstado() {
        _guardarEstado.value = GuardarEstado.Idle
    }

    // === MÉTODOS DE COMPARTIR ===

    /**
     * Indica si se debe mostrar el diálogo para guardar antes de compartir.
     */
    private val _mostrarDialogoGuardarAntesDeCompartir = MutableStateFlow(false)
    val mostrarDialogoGuardarAntesDeCompartir: StateFlow<Boolean> = _mostrarDialogoGuardarAntesDeCompartir.asStateFlow()

    /**
     * Verifica si hay cambios sin guardar en el documento.
     * @return true si hay comandos en el historial de undo/redo.
     */
    fun haycambiosSinGuardar(): Boolean {
        return undoRedoManager.puedeDeshacer.value
    }

    /**
     * Comparte el PDF. Si hay cambios sin guardar, pregunta al usuario.
     * @param context Contexto de la aplicación.
     */
    fun compartir(context: android.content.Context) {
        if (haycambiosSinGuardar()) {
            _mostrarDialogoGuardarAntesDeCompartir.value = true
        } else {
            compartirDirecto(context)
        }
    }

    /**
     * Cierra el diálogo de guardar antes de compartir.
     */
    fun cerrarDialogoGuardarAntesDeCompartir() {
        _mostrarDialogoGuardarAntesDeCompartir.value = false
    }

    /**
     * Guarda y luego comparte el PDF.
     * @param context Contexto de la aplicación.
     */
    fun guardarYCompartir(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfPath = pdfRepository.getPdfFilePath()
            if (pdfPath == null) {
                _guardarEstado.value = GuardarEstado.Error("No hay archivo PDF abierto")
                return@launch
            }

            _guardarEstado.value = GuardarEstado.Guardando("Guardando...")

            val layers = layerRepository.obtenerTodasLasCapas()
            val resultado = saveRepository.guardarSuspend(pdfPath, layers)

            when (resultado) {
                is SaveResult.Exito -> {
                    _guardarEstado.value = GuardarEstado.Idle
                    undoRedoManager.limpiar()
                    limpiarBackups(pdfPath)
                    _mostrarDialogoGuardarAntesDeCompartir.value = false
                    compartirDirecto(context)
                }
                is SaveResult.Error -> {
                    _guardarEstado.value = GuardarEstado.Error(resultado.mensaje)
                }
            }
        }
    }

    /**
     * Comparte el PDF directamente sin guardar.
     * @param context Contexto de la aplicación.
     */
    fun compartirDirecto(context: android.content.Context) {
        val pdfPath = pdfRepository.getPdfFilePath()
        if (pdfPath != null) {
            val uri = Uri.fromFile(File(pdfPath))
            ShareUtils.compartirPdf(context, uri)
        }
        _mostrarDialogoGuardarAntesDeCompartir.value = false
    }

    /**
     * Limpia los archivos de backup temporales.
     */
    private fun limpiarBackups(pdfPath: String) {
        try {
            File("$pdfPath.backup").delete()
            File("$pdfPath.undobackup").delete()
        } catch (e: Exception) {
            // Ignorar errores al limpiar backups
        }
    }

    override fun onCleared() {
        super.onCleared()
        undoRedoManager.limpiar()
        cerrarPdf()
    }
}

/**
 * Estados posibles para operaciones de página.
 */
sealed class PageOperationState {
    /** Estado inicial, sin operación en curso. */
    data object Idle : PageOperationState()
    /** Operación en curso con un mensaje. */
    data class Loading(val mensaje: String) : PageOperationState()
    /** Operación exitosa con un mensaje. */
    data class Success(val mensaje: String) : PageOperationState()
    /** Operación fallida con un mensaje de error. */
    data class Error(val mensaje: String) : PageOperationState()
}

/**
 * Estados posibles para operaciones de guardado.
 */
sealed class GuardarEstado {
    /** Estado inicial, sin operación en curso. */
    data object Idle : GuardarEstado()
    /** Operación de guardado en curso. */
    data class Guardando(val mensaje: String) : GuardarEstado()
    /** Guardado exitoso. */
    data object Exito : GuardarEstado()
    /** Guardado exitoso con nueva ruta (guardar como). */
    data class ExitoGuardarComo(val ruta: String) : GuardarEstado()
    /** Error con mensaje descriptivo. */
    data class Error(val mensaje: String) : GuardarEstado()
}