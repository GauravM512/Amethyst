package dev.anthonyhfm.amethyst.start.ui

import amethyst.composeapp.generated.resources.Res
import amethyst.composeapp.generated.resources.amethyst_studio_logo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

@Composable
fun AmethystWelcome() {
    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        Image(
            painter = painterResource(Res.drawable.amethyst_studio_logo),
            contentDescription = "Amethyst Studio Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
        )
    }
}