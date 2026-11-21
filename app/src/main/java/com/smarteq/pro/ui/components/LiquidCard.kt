package com.smarteq.pro.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.smarteq.pro.ui.theme.LocalPeach
import kotlin.math.max

/**
 * The "liquid" surface used for every card: the card colour with soft pools of the
 * harmonising accents (peach, blush, lilac, butter) underneath a faint glossy sheen, a
 * thin edge, and a soft drop shadow.
 *
 * Gradients are built once per size (drawWithCache), so this costs little more than a flat
 * card to draw. [variant] shifts where the colour pools sit so neighbouring cards differ.
 * [inset] gives a flatter, quieter version for cards nested inside other cards.
 */
@Composable
fun LiquidCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    variant: Int = 0,
    inset: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalPeach.current
    Column(
        modifier = modifier
            .then(if (inset) Modifier else Modifier.shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f)))
            .clip(shape)
            .liquidBackground(variant, inset)
            .border(if (selected) 2.dp else 1.dp, if (selected) t.fill else t.line, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        content = content,
    )
}

/** Paints the liquid surface behind content. Usable on any shaped component, e.g. a bottom bar. */
@Composable
fun Modifier.liquidBackground(variant: Int = 0, inset: Boolean = false): Modifier {
    val t = LocalPeach.current
    val base = if (inset) t.soft else t.card
    val alpha = if (inset) t.poolAlpha * 0.6f else t.poolAlpha
    val colors = t.harmony
    return this.drawWithCache {
        val w = size.width
        val h = size.height
        val big = max(w, h)
        // Pool centres (fraction of width, height) and radii (fraction of the longer side).
        val spots = listOf(
            Triple(0.12f, 0.18f, 0.60f),
            Triple(0.90f, 0.30f, 0.52f),
            Triple(0.30f, 1.00f, 0.62f),
            Triple(1.00f, 0.95f, 0.48f),
        )
        val pools = (0 until 3).map { i ->
            val c = colors[(i + variant) % colors.size]
            val (x, y, r) = spots[(i + variant) % spots.size]
            Brush.radialGradient(
                colors = listOf(c.copy(alpha = alpha), c.copy(alpha = 0f)),
                center = Offset(w * x, h * y),
                radius = big * r,
            )
        }
        val sheen = Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (t.isDark) 0.07f else 0.35f),
            0.4f to Color.White.copy(alpha = 0f),
        )
        onDrawBehind {
            drawRect(base)
            pools.forEach { drawRect(it) }
            drawRect(sheen)
        }
    }
}
