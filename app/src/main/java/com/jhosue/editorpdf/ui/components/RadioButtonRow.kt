package com.jhosue.editorpdf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Componente reutilizable para una fila con RadioButton y etiqueta de texto.
 */
@Composable
fun RadioButtonRow(
    label: String,
    index: Int,
    selectedIndex: Int,
    onClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick(index) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = index == selectedIndex,
            onClick = { onClick(index) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
