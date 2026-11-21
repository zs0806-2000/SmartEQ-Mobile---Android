#!/usr/bin/env python3
"""Re-run after any design-system change:  python3 design_tokens_check.py <repo>
Checks: colours hard-coded outside ui/theme; colors.xml vs Color.kt; every typography and
colour role used in code is defined (both schemes); WCAG contrast of the main role pairs."""
import re, sys, pathlib
root = pathlib.Path(sys.argv[1]); src = root / "app/src/main"
theme = src / "java/com/smarteq/pro/ui/theme"
kt = {p: p.read_text() for p in (src / "java").rglob("*.kt")}
fail = 0
def bad(m):
    global fail; fail += 1; print("FAIL", m)

for p, t in kt.items():
    if theme in p.parents: continue
    for i, line in enumerate(t.splitlines(), 1):
        if re.search(r"Color\(0x|Brand(Pink|Purple|Teal)", line) and "import" not in line:
            bad(f"hard-coded colour outside theme: {p.relative_to(root)}:{i}")

colorkt = (theme / "Color.kt").read_text()
named = {m.group(1): m.group(2).upper() for m in re.finditer(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", colorkt)}
xml = (src / "res/values/colors.xml").read_text()
for kname, xname in (("BgDark", "surface_deepest"), ("BgLight", "surface_light"), ("Peach", "brand_peach")):
    m = re.search(rf'name="{xname}">#([0-9A-Fa-f]{{6}})<', xml)
    if not m or m.group(1).upper() != named.get(kname): bad(f"colors.xml {xname} != Color.kt {kname}")

typ = (theme / "Type.kt").read_text()
for r in sorted({m for t in kt.values() for m in re.findall(r"typography\.(\w+)", t)}):
    if not re.search(rf"\b{r}\s*=", typ): bad(f"typography.{r} not defined")

th = (theme / "Theme.kt").read_text()
def scheme(start):
    blk = th[th.index(start):]; blk = blk[:blk.index("\n)\n")]
    out = {}
    for k, v in re.findall(r"(\w+) = ([^,\n]+),", blk):
        v = v.strip()
        h = re.match(r"Color\(0xFF([0-9A-Fa-f]{6})\)", v)
        out[k] = h.group(1).upper() if h else ("FFFFFF" if v == "Color.White" else "000000" if v == "Color.Black" else named.get(v))
    return out
dark, light = scheme("darkColorScheme("), scheme("lightColorScheme(")
for r in sorted({m for t in kt.values() for m in re.findall(r"colorScheme\.(\w+)", t)}):
    for n, s in (("dark", dark), ("light", light)):
        if r not in s: bad(f"colorScheme.{r} not set in {n} scheme")

def lum(h):
    c = [int(h[i:i+2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
def cr(a, b): a, b = lum(a), lum(b); return (max(a, b) + .05) / (min(a, b) + .05)
pairs = [("onPrimary", "primary", 4.5), ("onSurface", "surface", 4.5), ("onSurfaceVariant", "surface", 4.5),
         ("onSurfaceVariant", "background", 4.5), ("onPrimaryContainer", "primaryContainer", 4.5),
         ("onBackground", "background", 4.5), ("primary", "surface", 3.0)]
for n, s in (("dark", dark), ("light", light)):
    for fg, bg, need in pairs:
        if s.get(fg) and s.get(bg) and cr(s[fg], s[bg]) < need:
            bad(f"{n}: {fg} on {bg} = {cr(s[fg], s[bg]):.2f}:1 (needs {need})")
print("OK" if not fail else f"{fail} problem(s)")
sys.exit(1 if fail else 0)
