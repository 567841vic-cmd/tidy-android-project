package tw.vic.tidy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tw.vic.tidy.ui.theme.T
import tw.vic.tidy.ui.theme.Ty

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Ty.Label, modifier = modifier)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(T.Ink800, RoundedCornerShape(10.dp))
            .border(1.dp, T.Line, RoundedCornerShape(10.dp))
            .padding(16.dp),
        content = content
    )
}

/** A labelled reading with a hairline meter under it. */
@Composable
fun StatLine(
    label: String,
    value: String,
    detail: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(label, style = Ty.BodyDim)
            Text(value, style = Ty.Number)
        }
        Spacer(Modifier.height(8.dp))
        ThinMeter(fraction, color)
        Spacer(Modifier.height(6.dp))
        Text(detail, style = Ty.Caption)
    }
}

@Composable
fun PrimaryAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = T.Brass,
            contentColor = T.Ink900,
            disabledContainerColor = T.Ink700,
            disabledContentColor = T.MistFaint
        ),
        modifier = modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(text, style = Ty.Title.copy(color = Color.Unspecified))
    }
}

@Composable
fun QuietAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text, style = Ty.Body.copy(color = T.Jade))
    }
}

/** A row that hands the user off to a system screen. */
@Composable
fun HandoffRow(
    title: String,
    detail: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Ty.Body)
            Spacer(Modifier.height(3.dp))
            Text(
                detail, style = Ty.Caption,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(actionLabel, style = Ty.Body.copy(color = T.Jade))
    }
}

@Composable
fun Divider1() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawRect(T.Line, Offset.Zero, Size(size.width, size.height))
    }
}

// ---- Navigation glyphs, drawn rather than imported ---------------------

@Composable
fun GlyphStrata(active: Boolean) {
    val c = if (active) T.Brass else T.MistFaint
    Canvas(Modifier.size(22.dp)) {
        val h = 3.5f * density
        val gap = 3.5f * density
        val widths = listOf(1f, 0.68f, 0.4f)
        var y = (size.height - (h * 3 + gap * 2)) / 2f
        widths.forEach { fr ->
            drawRect(c, Offset(0f, y), Size(size.width * fr, h))
            y += h + gap
        }
    }
}

@Composable
fun GlyphFunnel(active: Boolean) {
    val c = if (active) T.Brass else T.MistFaint
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.18f)
            lineTo(w * 0.95f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.56f)
            lineTo(w * 0.58f, h * 0.95f)
            lineTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.56f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
fun GlyphCell(active: Boolean) {
    val c = if (active) T.Brass else T.MistFaint
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val top = h * 0.16f
        val bodyH = h * 0.68f
        drawRect(c, Offset(w * 0.42f, h * 0.06f), Size(w * 0.16f, h * 0.08f))
        drawRect(c, Offset(w * 0.2f, top), Size(w * 0.6f, 2f))
        drawRect(c, Offset(w * 0.2f, top + bodyH - 2f), Size(w * 0.6f, 2f))
        drawRect(c, Offset(w * 0.2f, top), Size(2f, bodyH))
        drawRect(c, Offset(w * 0.8f - 2f, top), Size(2f, bodyH))
        drawRect(
            c,
            Offset(w * 0.26f, top + bodyH * 0.45f),
            Size(w * 0.48f, bodyH * 0.45f)
        )
    }
}
