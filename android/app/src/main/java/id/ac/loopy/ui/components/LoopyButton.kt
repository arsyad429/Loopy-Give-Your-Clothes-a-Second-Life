package id.ac.loopy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LoopyButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    val shape = RoundedCornerShape(14.dp)

    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape) {
            Text(label)
        }
    } else {
        Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape) {
            Text(label)
        }
    }
}
