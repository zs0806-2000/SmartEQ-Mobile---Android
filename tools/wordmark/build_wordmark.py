"""Build the SmartyQ wordmark (approved concept QC "Carved bowl").

Usage:
    python3 tools/wordmark/build_wordmark.py <BricolageGrotesque variable .ttf>

The font is not bundled in the app: the wordmark ships as outlines. Get the
font (SIL OFL) from github.com/google/fonts, ofl/bricolagegrotesque.

What it does:
  1. Instances Bricolage Grotesque at opsz 96, wdth 88, wght 720.
  2. Shapes "SmartyQ" with HarfBuzz, so spacing and kerning are the font's own.
  3. Replaces the Q with the approved version: the bowl is filled solid, like
     the brain in the icon, and three EQ bands are cut out of it.
  4. Removes overlaps (skia-pathops), so every outline is clean and fills the
     same way in any app.
  5. Writes the vector masters in design/wordmark/ and the Compose path data
     in ui/components/Wordmark.kt.

Needs: fonttools, uharfbuzz, skia-pathops, rsvg-convert (for PDF/PNG).
"""
import io, re, subprocess, sys
from pathlib import Path
import pathops
import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.recordingPen import RecordingPen

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "design" / "wordmark"
KOTLIN = ROOT / "app/src/main/java/com/smarteq/pro/ui/components/Wordmark.kt"
ICON = ROOT / "design/icon/smarteq-icon.svg"

AXES = dict(opsz=96, wdth=88, wght=720)
TEXT = "SmartyQ"
K = 0.5523  # circle-to-cubic constant

# Q bands in font units: (x, bottom, width, height). Bottom-aligned, tallest in
# the middle, like the bands in the icon.
BANDS = [(182, 130, 44, 250), (252, 130, 44, 400), (322, 130, 44, 320)]

# Colours from the design system (DESIGN.md section 2).
INK_DARK, INK_LIGHT, PEACH_INK = "#FBF4F1", "#1E1517", "#B5532C"
GRADIENT = ("#FFB599", "#F7B7C6", "#CDB9F2")


def pill(pen, x, y0, w, h):
    r = w / 2; x1 = x + w; yb, yt = y0 + r, y0 + h - r
    pen.moveTo((x1, yb)); pen.lineTo((x1, yt))
    pen.curveTo((x1, yt + K*r), (x + r + K*r, y0 + h), (x + r, y0 + h))
    pen.curveTo((x + r - K*r, y0 + h), (x, yt + K*r), (x, yt))
    pen.lineTo((x, yb))
    pen.curveTo((x, yb - K*r), (x + r - K*r, y0), (x + r, y0))
    pen.curveTo((x + r + K*r, y0), (x1, yb - K*r), (x1, yb))
    pen.closePath()


def build(font_path):
    tt = instantiateVariableFont(TTFont(font_path), AXES)
    buf_bytes = io.BytesIO(); tt.save(buf_bytes); data = buf_bytes.getvalue()
    tt = TTFont(io.BytesIO(data)); gs = tt.getGlyphSet(); order = tt.getGlyphOrder()
    face = hb.Face(data); font = hb.Font(face)
    buf = hb.Buffer(); buf.add_str(TEXT); buf.guess_segment_properties()
    hb.shape(font, buf, {"kern": True})

    smarty, q = pathops.Path(), pathops.Path()
    x = 0
    items = list(zip(buf.glyph_infos, buf.glyph_positions))
    for i, (info, pos) in enumerate(items):
        name = order[info.codepoint]
        is_q = i == len(items) - 1
        target = pathops.Path()
        tp = TransformPen(target.getPen(), (1, 0, 0, 1, x + pos.x_offset, pos.y_offset))
        if is_q:
            rec = RecordingPen(); gs[name].draw(rec)
            split = next(j for j, (op, _) in enumerate(rec.value) if op == "closePath") + 1
            for op, a in rec.value[:split]: getattr(tp, op)(*a)    # outer contour only: solid bowl
            cut = pathops.Path(); ctp = TransformPen(cut.getPen(), (1, 0, 0, 1, x + pos.x_offset, pos.y_offset))
            for b in BANDS: pill(ctp, *b)
            target = pathops.op(target, cut, pathops.PathOp.DIFFERENCE)
            q = target
        else:
            gs[name].draw(tp)
            smarty = pathops.op(smarty, target, pathops.PathOp.UNION)
        x += pos.x_advance
    return smarty, q


def to_svg_d(path, minx, maxy, scale):
    """Font units (y up) -> viewport units (y down), origin at top-left of bounds."""
    pen = SVGPathPen(None, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
    path.draw(TransformPen(pen, (scale, 0, 0, -scale, -minx * scale, maxy * scale)))
    return pen.getCommands()


def main():
    smarty, q = build(sys.argv[1])
    b1, b2 = smarty.bounds, q.bounds
    minx, miny = min(b1[0], b2[0]), min(b1[1], b2[1])
    maxx, maxy = max(b1[2], b2[2]), max(b1[3], b2[3])
    scale = 0.1                                  # 1 viewport unit = 10 font units
    W, H = (maxx - minx) * scale, (maxy - miny) * scale
    d_s, d_q = to_svg_d(smarty, minx, maxy, scale), to_svg_d(q, minx, maxy, scale)
    baseline = maxy * scale                      # y of the baseline inside the viewport
    DESIGN.mkdir(parents=True, exist_ok=True)

    def svg(ink, qfill, defs="", bg=None, pad=0):
        vb = f"{-pad:.1f} {-pad:.1f} {W + 2*pad:.1f} {H + 2*pad:.1f}"
        rect = f'<rect x="{-pad}" y="{-pad}" width="{W+2*pad:.1f}" height="{H+2*pad:.1f}" fill="{bg}"/>' if bg else ""
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{(W+2*pad)*8:.0f}" height="{(H+2*pad)*8:.0f}">'
                f'<title>SmartyQ</title>{defs}{rect}'
                f'<g id="Smarty"><path d="{d_s}" fill="{ink}"/></g>'
                f'<g id="Q"><path d="{d_q}" fill="{qfill}"/></g></svg>\n')

    grad = (f'<defs><linearGradient id="qg" gradientUnits="userSpaceOnUse" x1="{b2[0]*scale - minx*scale:.1f}" y1="0" '
            f'x2="{W:.1f}" y2="{H:.1f}"><stop offset="0" stop-color="{GRADIENT[0]}"/>'
            f'<stop offset=".55" stop-color="{GRADIENT[1]}"/><stop offset="1" stop-color="{GRADIENT[2]}"/></linearGradient></defs>')
    files = {
        "smartyq-wordmark-on-dark.svg": svg(INK_DARK, "url(#qg)", grad),
        "smartyq-wordmark-on-light.svg": svg(INK_LIGHT, PEACH_INK),
        "smartyq-wordmark-mono-black.svg": svg("#000000", "#000000"),
        "smartyq-wordmark-mono-white.svg": svg("#FFFFFF", "#FFFFFF"),
        "smartyq-wordmark-preview-dark.svg": svg(INK_DARK, "url(#qg)", grad, bg="#141113", pad=12),
    }
    for name, content in files.items():
        (DESIGN / name).write_text(content)

    # Lockup: icon + wordmark, icon height = 1.6 x cap height, gap = 0.5 x icon.
    icon_svg = ICON.read_text()
    inner = re.sub(r'^<svg[^>]*>|</svg>\s*$', '', icon_svg.strip())
    cap = 66.0 * 1.0                             # cap height 660 font units x 0.1
    icon_h = cap * 1.6; gap = icon_h * 0.32
    lw = icon_h + gap + W; lh = max(icon_h, H)
    ty = (icon_h - cap) / 2 - (baseline - cap) if icon_h > H else 0
    wy = (icon_h / 2) - (baseline - cap / 2)
    for tag, ink, qfill, bg in (("dark", INK_DARK, "url(#qg)", "#141113"), ("light", INK_LIGHT, PEACH_INK, "#FBF5F2")):
        pad = 16
        lock = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{-pad} {-pad} {lw+2*pad:.1f} {icon_h+2*pad:.1f}" '
                f'width="{(lw+2*pad)*8:.0f}" height="{(icon_h+2*pad)*8:.0f}"><title>SmartyQ</title>'
                f'{grad if qfill.startswith("url") else ""}'
                f'<rect x="{-pad}" y="{-pad}" width="{lw+2*pad:.1f}" height="{icon_h+2*pad:.1f}" fill="{bg}"/>'
                f'<clipPath id="ic"><rect width="{icon_h:.1f}" height="{icon_h:.1f}" rx="{icon_h*0.23:.1f}"/></clipPath>'
                f'<g id="Icon" clip-path="url(#ic)"><svg width="{icon_h:.1f}" height="{icon_h:.1f}" viewBox="0 0 108 108">{inner}</svg></g>'
                f'<g id="Wordmark" transform="translate({icon_h+gap:.1f},{wy:.1f})">'
                f'<path d="{d_s}" fill="{ink}"/><path d="{d_q}" fill="{qfill}"/></g></svg>\n')
        (DESIGN / f"smartyq-lockup-{tag}.svg").write_text(lock)

    for name in ["smartyq-wordmark-on-dark", "smartyq-wordmark-on-light", "smartyq-lockup-dark", "smartyq-lockup-light"]:
        src = DESIGN / f"{name}.svg"
        subprocess.run(["rsvg-convert", "-f", "pdf", "-o", str(DESIGN / f"{name}.pdf"), str(src)], check=True)
        subprocess.run(["rsvg-convert", "-o", str(DESIGN / f"{name}.png"), str(src)], check=True)

    KOTLIN.write_text(f'''package com.smarteq.pro.ui.components

// GENERATED by tools/wordmark/build_wordmark.py. Edit the script, not this file.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarteq.pro.ui.theme.Blush
import com.smarteq.pro.ui.theme.Lilac
import com.smarteq.pro.ui.theme.LocalPeach
import com.smarteq.pro.ui.theme.Peach
import com.smarteq.pro.ui.theme.PeachInk

/**
 * The SmartyQ wordmark (concept QC "Carved bowl"), drawn from outlines of
 * Bricolage Grotesque (SIL OFL), so no extra font is bundled.
 *
 * "Smarty" uses the current text colour. The Q uses the icon gradient on dark
 * backgrounds and PeachInk on light ones, where the gradient is too pale.
 *
 * @param height height of the whole mark (cap top to the bottom of the Q's tail).
 *   The cap height is about {66.0 / H:.2f} of this.
 */
@Composable
fun SmartyQWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    ink: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
) {{
    val dark = LocalPeach.current.isDark
    val smarty = remember {{ PathParser().parsePathString(SMARTY).toPath().apply {{ fillType = PathFillType.NonZero }} }}
    val q = remember {{ PathParser().parsePathString(Q).toPath().apply {{ fillType = PathFillType.NonZero }} }}
    val width = height * (VIEW_W / VIEW_H)
    Canvas(
        modifier
            .width(width)
            .height(height)
            .semantics {{ contentDescription = "SmartyQ" }}
    ) {{
        val s = size.height / VIEW_H
        scale(s, s, pivot = Offset.Zero) {{
            drawPath(smarty, ink)
            if (dark) {{
                drawPath(q, Brush.linearGradient(listOf(Peach, Blush, Lilac), start = Offset(Q_START_X, 0f), end = Offset(VIEW_W, VIEW_H)))
            }} else {{
                drawPath(q, PeachInk)
            }}
        }}
    }}
}}

private const val VIEW_W = {W:.2f}f
private const val VIEW_H = {H:.2f}f
private const val Q_START_X = {b2[0]*scale - minx*scale:.2f}f

private const val SMARTY =
    "{d_s}"

private const val Q =
    "{d_q}"
''')
    print(f"viewport {W:.1f} x {H:.1f}, baseline at y={baseline:.1f}")
    print("wrote", ", ".join(sorted(p.name for p in DESIGN.iterdir())))
    print("wrote", KOTLIN.relative_to(ROOT))


if __name__ == "__main__":
    main()
