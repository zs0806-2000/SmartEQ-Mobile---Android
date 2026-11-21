# SmartyQ — Design System: "Peach Pulse"

Status: **approved 2026-11-21**. Platform: Android (Jetpack Compose, Material 3).
Source of truth in code: `app/src/main/java/com/smarteq/pro/ui/theme/` and `ui/components/LiquidCard.kt`.
If this file and the code disagree, the code wins. Fix this file.

---

## 1. Principles

1. **Warm, soft, tactile.** Peach-led pastels on deep warm neutrals. Controls feel like physical hardware (shadowed knobs and faders) sitting on liquid-glass-like cards.
2. **One accent, three harmonies.** Peach is the brand colour. Blush, lilac and butter are used to blend and pool, never as competing brand colours.
3. **Readable first.** Every text and colour pair passes WCAG AA. Numbers use tabular figures so dB values line up.
4. **The user's own choices win.** Theme (light/dark) and font are user settings. Nothing in the UI hard-codes either.
5. **No generic "AI app" look.** That means no gradient washes behind whole screens, no emoji icons, and no left-border accent cards.

---

## 2. Colour

### 2.1 Brand and harmonies

| Name | Hex | Use |
|---|---|---|
| Peach | `#FFB599` | Brand accent; dark-mode primary |
| Blush | `#F7B7C6` | Harmony; dark-mode secondary |
| Lilac | `#CDB9F2` | Harmony; dark-mode tertiary |
| Butter | `#FFD8A8` | Harmony (liquid pools only) |
| PeachDeep | `#D9714A` | Light-mode fills (sliders, active switches); 3:1+ on white |
| PeachInk | `#B5532C` | Light-mode primary (button fills); white text passes 4.5:1 |
| OnPeach | `#2A140C` | Text and icons on Peach |

### 2.2 Surfaces and text

| Token | Dark | Light | Material role |
|---|---|---|---|
| Background | `#141113` | `#FBF5F2` | `background` |
| Card | `#1E191C` | `#FFFFFF` | `surface`, `surfaceContainer` |
| Soft | `#2A2327` | `#F3E8E3` | `surfaceVariant`, `surfaceContainerHigh`, fader track |
| Line | `#3A3136` | `#E9DBD5` | `outlineVariant`, card edges, 0 dB line |
| Ink | `#FBF4F1` | `#1E1517` | `onSurface`, `onBackground` |
| Ink 2 | `#D2C4C0` | `#54474A` | secondary text |
| Muted | `#BFB2AE` | `#75666A` | `onSurfaceVariant` (captions, labels) |
| Tint | `#3D2820` | `#FFE6DB` | `primaryContainer` (chips, nav indicator) |
| Accent text | `#FFC4AC` | `#9C4527` | `onPrimaryContainer`, links |
| Error | `#FF8A80` | `#B3261E` | `error` |

Full Material 3 schemes (all roles) live in `Theme.kt` (`DarkColors`, `LightColors`).
Tokens Material has no slot for live in `PeachTokens`, read with `LocalPeach.current`: `card`, `soft`, `line`, `tint`, `accentText`, `fill`, `harmony`, `poolAlpha`, `isDark`.

### 2.3 Fader / band colour

- **Dark mode:** each band blends across Peach → Blush → Lilac from lowest to highest frequency (`bandColor(index, total, dark = true)`).
- **Light mode:** every band uses PeachDeep (pastels disappear on white).

### 2.4 Rules

- Never write a hex value outside `ui/theme/`. Use `MaterialTheme.colorScheme.*` or `LocalPeach.current.*`.
- Window and splash colours in `res/values/colors.xml` must match `BgDark` / `BgLight` / `Peach`. The check script enforces this.
- Pastel accents are for **dark** surfaces and **fills**. On light surfaces use PeachDeep or PeachInk.

---

## 3. Typography

### 3.1 Fonts (user-selectable, Settings › Appearance)

All bundled in `res/font/` as variable TTFs (one file covers weights 400–800). Licence: SIL Open Font License 1.1; texts are in `assets/licenses/fonts-OFL.txt`. No font is ever downloaded.

| Option | Headings | Text | Heading weight |
|---|---|---|---|
| **Sora** (default) | Sora | Sora | SemiBold 600 |
| Urbanist | Urbanist | Urbanist | ExtraBold 800 |
| Outfit | Outfit | Outfit | Bold 700 |
| Lexend | Lexend | Lexend | SemiBold 600 |
| Red Hat | Red Hat Display | Red Hat Text | ExtraBold 800 |
| Albert Sans | Albert Sans | Albert Sans | Bold 700 |
| Readex Pro | Readex Pro | Readex Pro | SemiBold 600 |
| Nunito | Nunito | Nunito | ExtraBold 800 |

Defined in `Fonts.kt` (`AppFont`).

### 3.2 Type scale (`typographyFor(font)` in `Type.kt`)

"H" = the font's heading face at its heading weight; "B" = its text face.

| Role | Face | Size / line | Weight | Tracking | Notes |
|---|---|---|---|---|---|
| displayLarge | H | 48 / 54 sp | heading | −1.0 | |
| displayMedium | H | 40 / 46 | heading | −0.8 | |
| displaySmall | H | 32 / 38 | heading | −0.6 | tabular figures (big stats) |
| headlineLarge | H | 30 / 36 | heading | −0.5 | screen titles |
| headlineMedium | H | 26 / 32 | heading | −0.4 | |
| headlineSmall | H | 22 / 28 | heading | −0.2 | |
| titleLarge | H | 20 / 26 | heading | 0 | |
| titleMedium | B | 16 / 22 | Bold | 0 | card titles |
| titleSmall | B | 14 / 20 | Bold | 0 | section labels |
| bodyLarge | B | 16 / 24 | Regular | 0 | |
| bodyMedium | B | 14 / 20 | Regular | 0 | default body |
| bodySmall | B | 12 / 17 | Regular | 0 | captions, descriptions |
| labelLarge | B | 14 / 18 | SemiBold | 0 | buttons; tabular |
| labelMedium | B | 12 / 16 | SemiBold | 0 | fader dB values; tabular |
| labelSmall | B | 11 / 14 | SemiBold | +0.4 | band labels, overlines; tabular |

Rules:
- Every role is defined; nothing falls back to the system font.
- Overlines (e.g. "SMARTEQ PRO" above a title) are labelSmall, upper case.
- Never set `fontFamily` directly in a screen. Use a typography role. The only exception is the font picker's own previews.

---

## 4. Shape

| Element | Radius |
|---|---|
| Large cards (EQ bank, adaptive, settings sections) | 28 dp |
| Standard cards, nested/inset cards, preset rows | 22–24 dp |
| Buttons, chips, nav bar, switches, pills, status badges | full (pill) |
| Fader track, fill, knob | full (circle) |
| Small icon tiles | 14 dp |

## 5. Spacing

Base unit 4 dp. In use:
- Screen edge padding: **16 dp**
- Space between cards/sections: **16 dp**
- Inside cards: **20 dp** (16 dp for compact cards)
- Between related controls: **8–12 dp**
- Minimum touch target: **48 dp** (fader hit area 32 × 198 dp)

## 6. Elevation and shadows

Soft and short. Shadows suggest something you can touch, not a floating sheet.

| Element | Shadow |
|---|---|
| LiquidCard | 10 dp, black 25 % ambient + spot |
| Fader knob | 5 dp, black 40 % |
| Fader fill | 6 dp glow in the band's own colour |
| Rotary knob | 6 dp, black 35 % |
| Filled / tonal buttons | 3 dp (1 dp pressed) |
| Filter chips | 2 dp |
| Inset (nested) cards | none |

## 7. Liquid surface

`LiquidCard(...)` and `Modifier.liquidBackground(variant, inset)`, both in `ui/components/LiquidCard.kt`.

Layers, back to front:
1. Base fill: Card (or Soft if `inset = true`).
2. Three radial "pools", each one of the harmony colours (Peach, Blush, Lilac, Butter, rotated by `variant`):
   - Alpha **0.20** in dark mode and **0.30** in light mode (×0.6 when inset).
   - Centres at (12 %, 18 %), (90 %, 30 %), (30 %, 100 %) and (100 %, 95 %) of the card, with radii of 60 %, 52 %, 62 % and 48 % of its longer side.
3. Top sheen: a vertical white gradient, 7 % (dark) or 35 % (light) at the top, fading to 0 by 40 % of the height.
4. Edge: 1 dp in Line colour. A selected card uses a 2 dp edge in the Fill colour.

Use `variant` 0–3 so neighbouring cards don't look identical. Gradients are cached per size (`drawWithCache`).

---

## 8. Components

| Component | Spec |
|---|---|
| **LiquidCard** | §7. Optional `onClick` (exposed as Role.Button) and `selected`. |
| **EQ fader** (`EqBandFader`) | 176 dp track, 6 dp wide, in Soft. 0 dB centre line in Line. Fill grows from the centre in the band colour, with a glow. 22 dp knob in Ink with a 3 dp ring in the band colour and a shadow. dB value above (labelMedium), band label below (labelSmall). TalkBack: "<band> band", value in dB, adjustable range. |
| **Rotary knob** (`EqKnob`) | 56 dp, Soft with a top sheen, inner 42 dp Card disc, Primary indicator; shadow. |
| **Buttons** | Material filled / tonal with soft elevation. Light-mode filled buttons use PeachInk with white text. |
| **Chips** | FilterChip, pill, 2 dp elevation. Selected = Tint background. |
| **Switches** | Material Switch; on = Primary (Peach dark / PeachInk light). |
| **Bottom navigation** | Liquid background (variant 2). Indicator = Tint, selected icon = Accent text. |
| **Status badge** ("LIVE", "TUNED") | Pill, Tint background with Accent text; on a dark hero card, Peach with OnPeach. |
| **Adaptive correction bars** | Boosts in Primary, cuts in Tertiary (Lilac dark); ±4 dB scale; zero line in Line. |

---

## 9. Theme modes

`ThemeMode`: **Match phone** (default), **Dark**, **Light**. This is a user setting (Settings › Appearance). Window/splash backgrounds follow the phone via `res/values` and `res/values-night`.

## 10. Accessibility

- Text contrast ≥ 4.5:1, including small grey labels over liquid pools (dark Muted was raised to `#BFB2AE` for this).
- Fills and controls ≥ 3:1 against their surface.
- Colours that must be told apart also differ in lightness.
- Touch targets ≥ 48 dp. Icon-only buttons have content descriptions.
- Help links: every feature has a "?" that opens a plain-language Help entry.

## 11. Checking it

Run after any design change:

```
python3 tools/design_tokens_check.py .
```

It fails on:
- colours hard-coded outside `ui/theme/`
- `colors.xml` drifting from `Color.kt`
- typography or colour roles used but not defined in both schemes
- role pairs below WCAG AA

Last result: **OK** (2026-11-21).

## 12. Open items

- A custom shadowed thumb for the adaptive Strength slider.
- Check that scrolling stays smooth with the liquid cards on lower-end phones.

## 13. App icon

Concept **T3 "Bands cut into the brain"** (approved 2026-11-21): a top-down brain in a Peach → Blush → Lilac gradient. Its six EQ bands, and the groove between the two halves, are negative space. Background: `#141113` with a soft peach glow.
- Masters: `design/icon/smarteq-icon-editable.svg` (layers: Guides, Background, Brain, Bands), `smarteq-icon.svg` (flattened), `smarteq-icon.pdf`, `play-store-icon-512.png`.
- Android resources (adaptive foreground/background, monochrome for themed icons, notification icon, legacy mipmaps) are generated by `python3 tools/build_icon.py`. Change the script, not the XML.
- The brain stays inside the 66-unit safe circle (max radius 31.4 of 108).

## 14. Name and wordmark

- **Name:** written **SmartyQ**: one word, capital S and capital Q, no space. This is the display name only; the application ID (`com.aistudio.smarteqpro.vqtzm`) and code package (`com.smarteq.pro`) are unchanged, so installed copies upgrade in place.
- **Wordmark:** concept **QC "Carved bowl"** (approved 2026-11-21). "SmartyQ" is set in Bricolage Grotesque (SIL OFL) at opsz 96, wdth 88, wght 720, then outlined. The Q's bowl is filled solid, like the brain in the icon, and three EQ bands are cut out of it. The bands are bottom-aligned, with the tallest in the middle.
- **Colour:** "Smarty" uses the text colour (`onSurface`). On dark backgrounds the Q uses the icon gradient (Peach → Blush → Lilac). On light backgrounds it uses PeachInk `#B5532C`, because the gradient is too pale there. Mono black and white versions are for single-colour use.
- **Not a UI font:** the wordmark ships as outlines, so Bricolage is not bundled. Never type the name in Bricolage for headings; UI text stays in the user's app font (§3).
- **Minimum size:** 16 dp tall. Below that the bands in the Q close up, so use the plain text "SmartyQ" instead.
- **Clear space:** keep at least the Q's width empty around the wordmark.
- **Lockup:** icon on the left at 1.6 × the cap height, with a gap of 0.32 × the icon size.
- **Where it appears:** the onboarding welcome screen (with the app icon) and the About line in Settings.
- **Files:** `design/wordmark/` contains the wordmark on dark and on light, mono black and white, and the dark and light lockups, as SVG, PDF and PNG. In Compose, use `SmartyQWordmark(height = …)` from `ui/components/Wordmark.kt`.
- **Regenerate:** `python3 tools/wordmark/build_wordmark.py <BricolageGrotesque variable .ttf>` rebuilds every file and the Kotlin path data. Change the script, not the outputs.

## 15. Credits

The token *structure* (neutral surfaces, single accent with tint/text variants, pill controls) was informed by the Dropway mobile UI kit. No colours, fonts, images or icons from that kit are used.
