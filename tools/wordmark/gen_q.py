"""SmartyQ in Bricolage Grotesque: three ways for the Q's bowl to echo the T3 icon.

QA  Fissure     - the bowl is split at the top, like the gap between hemispheres.
QB  Bands in    - the normal Q, with three EQ bars standing in its counter.
QC  Carved bowl - the bowl is solid (like the brain) and three bands are cut
                  out of it as negative space, exactly as in the icon.
"""
import io, re, sys, html
from pathlib import Path
import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.recordingPen import RecordingPen

FONT = Path(sys.argv[1])          # static instance: opsz 96, wdth 88, wght 720
OUT = Path(sys.argv[2])
ICON = Path(sys.argv[3]).read_text()
VECT = Path(sys.argv[4]) if len(sys.argv) > 4 else None

DATA = FONT.read_bytes()
TT = TTFont(io.BytesIO(DATA))
GS = TT.getGlyphSet()
ORDER = TT.getGlyphOrder()
K = 0.5523

def pill(pen, x, y0, w, h):
    """Vertical pill (fully rounded ends), counter-clockwise, in font units."""
    r = w / 2
    x1 = x + w
    yb, yt = y0 + r, y0 + h - r
    pen.moveTo((x1, yb)); pen.lineTo((x1, yt))
    pen.curveTo((x1, yt + K*r), (x + r + K*r, y0 + h), (x + r, y0 + h))
    pen.curveTo((x + r - K*r, y0 + h), (x, yt + K*r), (x, yt))
    pen.lineTo((x, yb))
    pen.curveTo((x, yb - K*r), (x + r - K*r, y0), (x + r, y0))
    pen.curveTo((x + r + K*r, y0), (x1, yb - K*r), (x1, yb))
    pen.closePath()

def rect(pen, x0, y0, x1, y1):
    pen.moveTo((x0, y0)); pen.lineTo((x0, y1)); pen.lineTo((x1, y1)); pen.lineTo((x1, y0)); pen.closePath()

# Three bands, bottom-aligned, heights rise toward the middle like the icon.
BANDS = [(182, 130, 44, 250), (252, 130, 44, 400), (322, 130, 44, 320)]

def draw_q(variant, pen, extra_pen):
    """Builds the Q with real boolean operations, so the outline is clean vector geometry."""
    import pathops
    rec = RecordingPen(); GS["Q"].draw(rec)
    ops = rec.value
    split = next(i for i, (op, _) in enumerate(ops) if op == "closePath") + 1
    def to_path(cmds):
        pp = pathops.Path(); pr = pp.getPen()
        for op, a in cmds: getattr(pr, op)(*a)
        return pp
    full = to_path(ops)
    if variant == "QA":
        cut = pathops.Path(); rect(cut.getPen(), 252, 500, 296, 700)
        pathops.op(full, cut, pathops.PathOp.DIFFERENCE).draw(pen)
    elif variant == "QB":
        full.draw(pen)
        for b in [(196, 150, 40, 200), (256, 150, 40, 330), (316, 150, 40, 260)]:
            pill(extra_pen, *b)
    elif variant == "QC":
        cut = pathops.Path(); cp = cut.getPen()
        for b in BANDS: pill(cp, *b)
        pathops.op(to_path(ops[:split]), cut, pathops.PathOp.DIFFERENCE).draw(pen)

def wordmark(variant, size, ink, q_fill, defs="", gid=""):
    face = hb.Face(DATA); font = hb.Font(face)
    buf = hb.Buffer(); buf.add_str("SmartyQ"); buf.guess_segment_properties()
    hb.shape(font, buf, {"kern": True})
    s = size / face.upem
    x = 0.0; parts = []
    infos = list(zip(buf.glyph_infos, buf.glyph_positions))
    for i, (info, pos) in enumerate(infos):
        name = ORDER[info.codepoint]
        t = (s, 0, 0, -s, (x + pos.x_offset) * s, -pos.y_offset * s)
        p = SVGPathPen(GS); tp = TransformPen(p, t)
        if i == len(infos) - 1 and variant:
            e = SVGPathPen(GS); te = TransformPen(e, t)
            draw_q(variant, tp, te)
            parts.append(f'<path d="{p.getCommands()}" fill="{q_fill}" fill-rule="evenodd"/>')
            if e.getCommands():
                parts.append(f'<path d="{e.getCommands()}" fill="{q_fill}"/>')
        else:
            GS[name].draw(tp)
            fill = q_fill if i == len(infos) - 1 else ink
            parts.append(f'<path d="{p.getCommands()}" fill="{fill}"/>')
        x += pos.x_advance
    w = x * s; pad = size * .12; h = size * 1.25; base = size * .95
    return (f'<svg role="img" aria-label="SmartyQ" width="{w+2*pad:.0f}" height="{h:.0f}" '
            f'viewBox="{-pad:.1f} {-base:.1f} {w+2*pad:.1f} {h:.1f}" style="display:block;overflow:visible">'
            f'{defs}{"".join(parts)}</svg>')

def icon(px):
    svg = re.sub(r'width="\d+" height="\d+"', f'width="{px}" height="{px}"', ICON, count=1)
    svg = svg.replace('id="g"', 'id="ig"').replace('url(#g)', 'url(#ig)').replace('id="p"', 'id="ip"').replace('url(#p)', 'url(#ip)')
    return svg.replace('<svg ', f'<svg aria-hidden="true" style="display:block;border-radius:{px*0.23:.0f}px;flex:none" ', 1)

GRAD = ('<defs><linearGradient id="qg" x1="0" y1="0" x2="1" y2="1">'
        '<stop offset="0" stop-color="#FFB599"/><stop offset=".55" stop-color="#F7B7C6"/>'
        '<stop offset="1" stop-color="#CDB9F2"/></linearGradient></defs>')

VARIANTS = [
    ("QA", "Fissure", "The top of the Q's bowl is split, like the gap between the brain's two halves. The quietest change; reads as a normal Q at a glance."),
    ("QB", "Bands inside", "A normal Q with three small EQ bars standing in its hollow. Clear story, but the bars blur together below ~20 px."),
    ("QC", "Carved bowl", "The bowl is solid like the brain, and three bands are cut out of it as negative space, the same idea as the icon. Strongest echo of T3."),
]

BOARD = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>SmartyQ {id}: {name}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Sora:wght@400;600&amp;display=swap">
<style>body{{margin:0}}</style>
</helmet>
<div style="width:640px;height:760px;box-sizing:border-box;background:#F3E8E3;font-family:Sora,system-ui,sans-serif;display:flex;flex-direction:column">
  <div style="background:#141113;height:240px;display:flex;align-items:center;justify-content:center;gap:24px">
    {icon_dark}{wm_dark}
  </div>
  <div style="background:#FBF5F2;height:160px;display:flex;align-items:center;justify-content:center;border-bottom:1px solid #E9DBD5">
    {wm_light}
  </div>
  <div style="padding:20px 32px 0">
    <div style="font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:#75666A;margin-bottom:10px">App bar (22 px) and notification (15 px)</div>
    <div style="background:#1E191C;border-radius:20px;padding:14px 18px;display:flex;align-items:center;gap:12px;box-shadow:0 6px 16px rgba(20,17,19,.18)">
      {icon_small}
      <div style="display:flex;flex-direction:column;gap:4px">{wm_small}
        <div style="font-size:13px;color:#BFB2AE">Adaptive tuning on · Learning your headphones</div></div>
    </div>
    <div style="margin-top:12px;background:#FFFFFF;border-radius:16px;padding:10px 14px;display:flex;align-items:center;gap:10px;box-shadow:0 4px 12px rgba(20,17,19,.10)">
      {wm_tiny}<div style="font-size:12px;color:#75666A">· now tuning “Midnight City”</div>
    </div>
  </div>
  <div style="padding:16px 32px 0;color:#1E1517">
    <div style="font-size:18px;font-weight:600;margin-bottom:6px">{id} · {name}</div>
    <div style="font-size:13px;line-height:1.5;color:#5E5054">{note}</div>
    <div style="font-size:11px;color:#75666A;margin-top:8px">Bricolage Grotesque · opsz 96, wdth 88, wght 720 · SIL Open Font License</div>
  </div>
</div>
</x-dc>
</body>
</html>
"""

for vid, name, note in VARIANTS:
    (OUT / f"Wordmark-{vid}.dc.html").write_text(BOARD.format(
        id=vid, name=name, note=html.escape(note),
        icon_dark=icon(84), wm_dark=wordmark(vid, 84, "#FBF4F1", "url(#qg)", GRAD),
        wm_light=wordmark(vid, 76, "#1E1517", "#B5532C"),
        icon_small=icon(40), wm_small=wordmark(vid, 22, "#FBF4F1", "#FFB599"),
        wm_tiny=wordmark(vid, 15, "#1E1517", "#B5532C")))
    if VECT:
        VECT.mkdir(exist_ok=True)
        (VECT / f"{vid}.svg").write_text(wordmark(vid, 200, "#FBF4F1", "url(#qg)", GRAD).replace('<svg ', '<svg xmlns="http://www.w3.org/2000/svg" ', 1))
    print("wrote", vid)
