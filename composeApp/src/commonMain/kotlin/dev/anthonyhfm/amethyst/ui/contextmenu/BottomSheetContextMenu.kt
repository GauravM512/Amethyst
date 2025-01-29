package dev.anthonyhfm.amethyst.ui.contextmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContextMenu(items: List<ContextMenuItem>) {
    var visible: Boolean by remember { mutableStateOf(false) }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = {
                visible = false
            }
        ) {
            Column {
                for (item in items) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item.text,
                            )
                        },
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}