# Environment assets

The two environment backgrounds are original project assets generated with OpenAI's
built-in image generation tool on 2026-08-08. They do not reuse an external game,
character, logo, or commercial asset pack.

## Alex's bedroom

Saved as `src/main/resources/assets/alex-bedroom.png`.

Prompt summary: a 16:9, elevated three-quarter-view, hand-painted pixel-art teenage
bedroom workshop at night, with a moonlit window, bed, electronics workbench,
component box, right-side door, warm/cool mixed lighting, dimensional furniture,
contact shadows, and a broad unobstructed wooden floor. No characters, UI, text,
logos, watermark, copied game assets, or side-scroller framing.

## Mira's electronics shop

Saved as `src/main/resources/assets/mira-shop.png`.

Prompt summary: a 16:9, elevated three-quarter-view, hand-painted pixel-art
neighborhood electronics shop, with a deep display counter, component shelves,
oscilloscopes, wire spools, warm hanging lamps, cyan equipment glows, a far-left
exit, and broad unobstructed checker-tile floor. No characters, UI, readable text,
logos, watermark, copied game assets, or flat front-elevation framing.

The game scales each source image to its 480×270 internal canvas once during asset
loading, then uses nearest-neighbor scaling for the final game window.

## Character sprite sheets

The built-in image generation tool also produced two project-ready character sheets
on removable chroma-key backgrounds. The keyed backgrounds were converted to alpha
locally and validated before integration.

- `src/main/resources/assets/characters/alex-sprites.png`: a 4-column × 3-row sheet.
  Columns are front, left, right and back; rows are idle and two walk phases.
- `src/main/resources/assets/characters/mira-sprites.png`: a 3-column × 2-row sheet.
  The first row contains idle variations and the second contains talking gestures.

Prompt summary: original, consistent, full-body hand-painted pixel characters matching
the corresponding generated room reference, with fixed scale and ground line across
cells, crisp silhouettes, no labels or dividers, and a perfectly flat magenta removal
background. Alex wears a navy jacket and red heart accent; Mira wears a teal work shirt
and cream electronics apron.
