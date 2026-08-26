package tw.vic.tidy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tw.vic.tidy.core.formatBytes
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.Ty
import androidx.compose.material3.Text

/**
 * One band per category, sized by share of the volume. Reclaimable bands are
 * hatched so they read as "loose material" rather than another colour to
 * decode. This is the one element the app is built around.
 */
data class Stratum(
    val label: String,
    val bytes: Long,
    val color: Color,
    val reclaimable: Boolean = false
)

@Composable
fun StratumBar(
    strata: List<Stratum>,
    sweep: Float,
    scanning: Boolean,
    modifier: Modifier = Modifier
) {
    val total = strata.sumOf { it.bytes }.coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(5.dp))
    ) {
        val w = size.width
        val h = size.height
        drawRect(T.Ink700, Offset.Zero, Size(w, h))

        var x = 0f
        strata.forEach { band ->
            val raw = (band.bytes.toDouble() / total * w).toFloat()
            val bandWidth = if (band.bytes > 0 && raw < 2f) 2f else raw
            if (bandWidth <= 0f) return@forEach

            drawRect(band.color, Offset(x, 0f), Size(bandWidth, h))

            if (band.reclaimable) {
                clipRect(left = x, top = 0f, right = x + bandWidth, bottom = h) {
                    var lineX = x - h
                    while (lineX < x + bandWidth + h) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.28f),
                            start = Offset(lineX, h),
                            end = Offset(lineX + h, 0f),
                            strokeWidth = 2.2f
                        )
                        lineX += 9f
                    }
                }
            }

            x += bandWidth
            if (x < w) {
                drawRect(T.Ink900, Offset(x - 1f, 0f), Size(1.5f, h))
            }
        }

        if (scanning) {
            val sx = w * sweep.coerceIn(0f, 1f)
            drawRect(
                color = T.Mist.copy(alpha = 0.06f),
                topLeft = Offset(0f, 0f),
                size = Size(sx, h)
            )
            drawLine(
                color = T.Mist.copy(alpha = 0.85f),
                start = Offset(sx, 0f),
                end = Offset(sx, h),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun StratumLegend(strata: List<Stratum>, modifier: Modifier = Modifier) {
    Column(modifier) {
        strata.filter { it.bytes > 0 }.forEach { band ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(
                    Modifier
                        .size(9.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) { drawRect(band.color) }
                Spacer(Modifier.width(10.dp))
                Text(
                    band.label,
                    style = Ty.BodyDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(formatBytes(band.bytes), style = Ty.Body)
            }
        }
    }
}

@Composable
fun ThinMeter(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
    ) {
        drawRect(T.Ink700, Offset.Zero, Size(size.width, size.height))
        drawRect(
            color,
            Offset.Zero,
            Size(size.width * fraction.coerceIn(0f, 1f), size.height)
        )
    }
}
