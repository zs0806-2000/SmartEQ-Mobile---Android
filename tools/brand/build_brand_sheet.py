"""SmartyQ brand sheet board for the design canvas (approved decisions only).

Usage: python3 tools/brand/build_brand_sheet.py <repo root> <output dir>
Then publish Brand-Sheet.dc.html to the design canvas, project/ folder."""
import re, sys, itertools
from pathlib import Path

REPO = Path(sys.argv[1]); OUT = Path(sys.argv[2])
_n = itertools.count()

def inline(path, width=None, height=None, label="SmartyQ"):
    s = Path(path).read_text().strip()
    k = next(_n)
    for gid in set(re.findall(r'id="([^"]+)"', s)):
        s = s.replace(f'id="{gid}"', f'id="{gid}{k}"').replace(f'url(#{gid})', f'url(#{gid}{k})')
    s = re.sub(r'<svg([^>]*?) width="[\d.]+" height="[\d.]+"', r'<svg\1', s, count=1)
    size = (f' width="{width}"' if width else "") + (f' height="{height}"' if height else "")
    aria = f'role="img" aria-label="{label}"' if label else 'aria-hidden="true"'
    return s.replace("<svg", f'<svg {aria}{size} style="display:block;flex:none"', 1)

ICON = REPO / "design/icon/smarteq-icon.svg"
WM = REPO / "design/wordmark"

def icon(px, r=0.23):
    return f'<div style="width:{px}px;height:{px}px;border-radius:{px*r:.0f}px;overflow:hidden;flex:none;box-shadow:0 8px 20px rgba(0,0,0,.35)">{inline(ICON, px, px, label="SmartyQ app icon")}</div>'

def swatch(hexv, name, role, dark_text):
    fg = "#2A140C" if not dark_text else "#FBF4F1"
    return (f'<div style="display:flex;flex-direction:column;gap:6px;width:118px">'
            f'<div style="height:64px;border-radius:16px;background:{hexv};border:1px solid rgba(255,255,255,.08);box-shadow:0 4px 10px rgba(0,0,0,.25)"></div>'
            f'<div style="font-size:12px;font-weight:600">{name}</div>'
            f'<div style="font-size:11px;color:#BFB2AE;font-variant-numeric:tabular-nums">{hexv} · {role}</div></div>')

DARK = [("#141113","Background","bg"),("#1E191C","Card","card"),("#2A2327","Soft","soft"),("#3A3136","Line","line"),
        ("#FBF4F1","Ink","text"),("#BFB2AE","Muted","text 2"),("#3D2820","Tint","container"),("#FFC4AC","Accent text","on tint")]
LIGHT = [("#FBF5F2","Background","bg"),("#FFFFFF","Card","card"),("#F3E8E3","Soft","soft"),("#E9DBD5","Line","line"),
         ("#1E1517","Ink","text"),("#75666A","Muted","text 2"),("#FFE6DB","Tint","container"),("#9C4527","Accent text","on tint")]
BRAND = [("#FFB599","Peach","primary (dark)"),("#F7B7C6","Blush","harmony"),("#CDB9F2","Lilac","harmony"),("#FFD8A8","Butter","harmony"),
         ("#D9714A","PeachDeep","fill (light)"),("#B5532C","PeachInk","primary (light)"),("#2A140C","OnPeach","text on peach")]

FONTS = [("Sora","Sora",True),("Urbanist","Urbanist",False),("Outfit","Outfit",False),("Lexend","Lexend",False),
         ("Red Hat","Red Hat Display",False),("Albert Sans","Albert Sans",False),("Readex Pro","Readex Pro",False),("Nunito","Nunito",False)]
GF = "&amp;".join("family=" + f.replace(" ", "+") + ":wght@400;600;700" for _, f, _ in FONTS)

LIQUID = ("background:radial-gradient(60% 55% at 12% 10%, rgba(255,181,153,.30) 0%, rgba(0,0,0,0) 70%),"
          "radial-gradient(55% 50% at 95% 65%, rgba(205,185,242,.24) 0%, rgba(0,0,0,0) 70%),"
          "radial-gradient(50% 45% at 40% 110%, rgba(247,183,198,.20) 0%, rgba(0,0,0,0) 70%),"
          "linear-gradient(180deg, rgba(255,255,255,.07) 0%, rgba(255,255,255,0) 28%), #1E191C;"
          "border:1px solid rgba(255,255,255,.07);box-shadow:0 10px 24px rgba(0,0,0,.35)")

def section(title, body, sub=""):
    return (f'<section style="margin-top:44px"><div style="display:flex;align-items:baseline;gap:12px;margin-bottom:16px">'
            f'<h2 style="margin:0;font-size:22px;font-weight:700">{title}</h2>'
            f'<span style="font-size:13px;color:#BFB2AE">{sub}</span></div>{body}</section>')

def rules(items):
    return '<ul style="margin:14px 0 0;padding-left:18px;font-size:13px;line-height:1.6;color:#D2C4C0">' + "".join(f"<li>{i}</li>" for i in items) + "</ul>"

fader_bars = "".join(
    f'<div style="width:10px;height:120px;border-radius:5px;background:#2A2327;position:relative">'
    f'<div style="position:absolute;left:0;right:0;{"bottom:50%;height:" + str(h) + "%" if h>0 else "top:50%;height:" + str(-h) + "%"};border-radius:5px;background:{c};box-shadow:0 0 10px {c}66"></div>'
    f'<div style="position:absolute;left:-5px;width:20px;height:20px;border-radius:10px;background:#FBF4F1;top:calc({50 - h}% - 10px);box-shadow:0 3px 8px rgba(0,0,0,.45)"></div></div>'
    for h, c in [(18,"#FFB599"),(30,"#FFB7A8"),(10,"#F9B7BB"),(-12,"#F7B7C6"),(-6,"#E5B8D9"),(14,"#D9B8E6"),(24,"#CDB9F2")])

html = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>SmartyQ brand sheet</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?{GF}&amp;display=swap">
<style>body{{margin:0}}</style>
</helmet>
<div style="width:1280px;height:2260px;box-sizing:border-box;padding:56px 64px;background:#141113;color:#FBF4F1;font-family:Sora,system-ui,sans-serif;overflow:hidden">

<header style="display:flex;justify-content:space-between;align-items:flex-end">
  <div>{inline(WM / "smartyq-lockup-dark.svg", height=96)}</div>
  <div style="text-align:right;font-size:13px;color:#BFB2AE;line-height:1.6">Brand &amp; design system<br>Approved 21 Nov 2026 · Peach Pulse<br>Source of truth: DESIGN.md</div>
</header>

{section("App icon", f'''
<div style="display:flex;gap:28px;align-items:flex-end">
  {icon(180)}{icon(120)}{icon(72)}{icon(48)}
  <div style="width:120px;height:120px;border-radius:60px;overflow:hidden;box-shadow:0 8px 20px rgba(0,0,0,.35)">{inline(ICON,120,120,label="Round icon mask")}</div>
  <div style="flex:1;margin-left:12px">{rules([
    "Concept T3: a top-down brain. The six EQ bands and the groove between the halves are cut out of it (negative space).",
    "Gradient Peach → Blush → Lilac on #141113, with a soft peach glow.",
    "Android adaptive icon: the brain stays inside the 66-unit safe zone, so every mask shape keeps it whole. A one-colour layer is included for themed icons.",
    "Masters: design/icon/. Rebuild the Android files with tools/build_icon.py."])}</div>
</div>''', "T3 · Bands cut into the brain")}

{section("Wordmark", f'''
<div style="display:grid;grid-template-columns:1fr 1fr;gap:20px">
  <div style="border-radius:28px;padding:44px;display:flex;align-items:center;justify-content:center;{LIQUID}">{inline(WM / "smartyq-wordmark-on-dark.svg", height=92)}</div>
  <div style="border-radius:28px;padding:44px;display:flex;align-items:center;justify-content:center;background:#FBF5F2;box-shadow:0 10px 24px rgba(0,0,0,.35)">{inline(WM / "smartyq-wordmark-on-light.svg", height=92)}</div>
  <div style="border-radius:20px;padding:22px;display:flex;align-items:center;justify-content:center;background:#000">{inline(WM / "smartyq-wordmark-mono-white.svg", height=40)}</div>
  <div style="border-radius:20px;padding:22px;display:flex;align-items:center;justify-content:center;background:#fff">{inline(WM / "smartyq-wordmark-mono-black.svg", height=40)}</div>
</div>
{rules([
  "Written SmartyQ: one word, capital S and Q. Bricolage Grotesque (opsz 96 · wdth 88 · wght 720), outlined.",
  "The Q's bowl is solid like the brain, with three EQ bands cut out of it (concept QC, Carved bowl).",
  "“Smarty” uses the text colour. The Q is the icon gradient on dark and PeachInk #B5532C on light.",
  "At least 16 dp tall; below that, write “SmartyQ” as plain text. Keep clear space of at least the Q's width on every side.",
  "Bricolage is for the wordmark only. Never use it for UI text."])}''', "QC · Carved bowl")}

{section("Colour", f'''
<div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#BFB2AE;margin-bottom:10px">Brand &amp; harmony</div>
<div style="display:flex;gap:14px;flex-wrap:wrap">{"".join(swatch(h,n,r,True) for h,n,r in BRAND)}</div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:28px;margin-top:26px">
  <div><div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#BFB2AE;margin-bottom:10px">Dark theme</div>
    <div style="display:flex;gap:14px;flex-wrap:wrap">{"".join(swatch(h,n,r,True) for h,n,r in DARK)}</div></div>
  <div><div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#BFB2AE;margin-bottom:10px">Light theme</div>
    <div style="display:flex;gap:14px;flex-wrap:wrap">{"".join(swatch(h,n,r,True) for h,n,r in LIGHT)}</div></div>
</div>
{rules(["Every text/background pair passes WCAG AA; tools/design_tokens_check.py checks this.",
        "EQ bands blend Peach → Blush → Lilac from bass to treble. Light theme fills use PeachDeep."])}''', "Peach Pulse")}

{section("Typography", f'''
<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px">
{"".join(f'<div style="border-radius:20px;padding:18px;{LIQUID if d else "background:#1E191C;border:1px solid #3A3136"}"><div style="font-family:{chr(39)}{f}{chr(39)},sans-serif;font-size:28px;font-weight:700;line-height:1.1">Aa 15 bands</div><div style="font-family:{chr(39)}{f}{chr(39)},sans-serif;font-size:13px;color:#BFB2AE;margin-top:8px">Adaptive tuning on · learning your headphones</div><div style="font-size:12px;margin-top:12px;font-weight:600;color:{"#FFC4AC" if d else "#FBF4F1"}">{n}{" · default" if d else ""}</div></div>' for n,f,d in FONTS)}
</div>
{rules(["You choose the UI font in Settings › Appearance; Sora is the default. All eight are bundled variable fonts under the SIL OFL, so nothing is downloaded.",
        "Every font has a full Material 3 type scale. Labels and numbers use tabular figures."])}''', "8 user-selectable · Sora default")}

{section("Surfaces &amp; controls", f'''
<div style="display:grid;grid-template-columns:1.2fr 1fr;gap:20px">
  <div style="border-radius:28px;padding:26px;{LIQUID}">
    <div style="display:flex;justify-content:space-between;align-items:center"><div style="font-size:17px;font-weight:600">Equalizer</div>
      <div style="font-size:12px;padding:6px 12px;border-radius:999px;background:#3D2820;color:#FFC4AC;box-shadow:0 3px 8px rgba(0,0,0,.3)">Adaptive</div></div>
    <div style="display:flex;justify-content:space-between;padding:22px 14px 6px">{fader_bars}</div>
  </div>
  <div style="display:flex;flex-direction:column;gap:14px">
    <div style="display:flex;gap:12px;align-items:center">
      <div style="padding:14px 26px;border-radius:999px;background:#FFB599;color:#2A140C;font-weight:600;box-shadow:0 6px 14px rgba(255,181,153,.30),0 2px 4px rgba(0,0,0,.35)">Save preset</div>
      <div style="padding:14px 22px;border-radius:999px;border:1px solid #3A3136;color:#FBF4F1;box-shadow:0 2px 6px rgba(0,0,0,.25)">Reset</div>
      <div style="padding:8px 14px;border-radius:999px;background:#2A2327;font-size:13px;box-shadow:0 2px 6px rgba(0,0,0,.3)">Bass</div>
    </div>
    {rules(["Cards use the liquid recipe: three soft colour pools, a glossy top sheen, a 1 dp edge and a soft shadow. This includes the bottom bar.",
            "Sliders, knobs, filled buttons and chips have soft shadows. Faders fill outward from 0 dB with a glow.",
            "Shapes: cards 22–28 dp, buttons, chips and nav are pills. Theme follows the phone by default; Dark or Light can be set in Settings."])}
  </div>
</div>''', "")}

</div>
</x-dc>
</body>
</html>
"""
(OUT / "Brand-Sheet.dc.html").write_text(html)
print("ok", len(html))
