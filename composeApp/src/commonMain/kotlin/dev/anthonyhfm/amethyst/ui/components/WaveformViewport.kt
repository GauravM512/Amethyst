package dev.anthonyhfm.amethyst.ui.components

internal data class WaveformViewport(
    val start: Double = 0.0,
    val span: Double = 1.0,
) {
    val end: Double
        get() = start + span

    fun positionAt(x: Float, width: Float): Double =
        start + x.toDouble() / width.coerceAtLeast(1f) * span

    fun screenX(position: Double, width: Float): Float =
        ((position - start) / span * width).toFloat()

    fun pan(delta: Double): WaveformViewport = copy(
        start = (start + delta).coerceIn(0.0, 1.0 - span),
    )

    fun zoom(
        scale: Float,
        anchorFraction: Double,
        minimumSpan: Double,
    ): WaveformViewport {
        val anchor = anchorFraction.coerceIn(0.0, 1.0)
        val nextSpan = (span / scale).coerceIn(minimumSpan, 1.0)

        return WaveformViewport(
            start = (start + anchor * (span - nextSpan)).coerceIn(0.0, 1.0 - nextSpan),
            span = nextSpan,
        )
    }

    companion object {
        fun fitSelection(
            selectionStart: Float,
            selectionEnd: Float,
            width: Float,
            padding: Float,
            minimumSpan: Double,
        ): WaveformViewport {
            if (width <= padding * 2f || selectionEnd <= selectionStart) {
                return WaveformViewport()
            }

            val span = (
                (selectionEnd.toDouble() - selectionStart) * width / (width - padding * 2f)
            ).coerceIn(minimumSpan, 1.0)

            return WaveformViewport(
                start = (selectionStart - span * padding / width).coerceIn(0.0, 1.0 - span),
                span = span,
            )
        }
    }
}

internal fun waveformEdgePanDelta(
    pointerX: Float,
    width: Float,
    padding: Float,
    span: Double,
    elapsedSeconds: Float,
): Double {
    if (padding <= 0f || width <= padding * 2f) {
        return 0.0
    }

    val speed = when {
        pointerX < padding -> -((padding - pointerX) / padding).coerceIn(0f, 1f)
        pointerX > width - padding -> ((pointerX - width + padding) / padding).coerceIn(0f, 1f)
        else -> 0f
    }

    return speed * span * elapsedSeconds * 1.5
}
