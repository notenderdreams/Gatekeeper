# Environment assets

The environment backgrounds are original project assets generated with OpenAI's
built-in image generation tool on 2026-08-08. They do not reuse an external game,
character, logo, or commercial asset pack.

## Alex's bedroom

Saved as `src/main/resources/assets/alex-bedroom.png`.

Prompt summary: a 16:9, elevated three-quarter-view, hand-painted pixel-art teenage
bedroom workshop at night, with a moonlit window, bed, electronics workbench,
component box, right-side door, warm/cool mixed lighting, dimensional furniture,
contact shadows, and a broad unobstructed wooden floor. No characters, UI, text,
logos, watermark, copied game assets, or side-scroller framing.

## Lantern Street

Saved as `src/main/resources/assets/night-street-long.png`.

Prompt summary: an extra-wide side-view, hand-painted pixel-art nighttime street
connecting Alex's warm home entrance at the far left to Mira's cyan-lit electronics
shop entrance at the far right. The extended middle includes an uninterrupted
sidewalk, garden wall, residential rooftops, trees, utility poles and wires, amber
street lighting, and restrained puddle reflections. No characters, vehicles,
readable signs, logos, UI, watermark, blocked entrances, or isometric perspective.

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

## LogicLens item

Saved as `src/main/resources/assets/items/logiclens.png`.

Prompt summary: an original hand-painted pixel-art portable circuit tester styled as
a worn neighborhood electronics-shop instrument, with a boxy charcoal and faded-teal
casing, brass hardware, physical sockets, a cream test control, indicator lamps, and
a small CRT waveform screen. It was generated on a flat magenta removal background,
converted to an alpha PNG, and downsampled with nearest-neighbor filtering for the
game's low-resolution reward screen. No text, logo, watermark, holograms, or futuristic
controls.

## Audio starter library

Downloaded under `src/main/resources/assets/audio/kenney/` for the upcoming Java audio
integration. These are source packs; select and rename individual clips as the game
audio map is implemented.

- `interface-sounds`: menu, dialogue, confirmation, error, open and close cues.
- `rpg-audio`: footsteps, doors, books, pickups and physical room sounds.
- `ui-audio`: switches, clicks and interface feedback.
- `music-jingles`: short CC0 musical stingers for title, crafting, success and ending.
- `audio/music/solitude-main.m4a`: the user-provided ambient main theme.
- `audio/music/solitude-main.wav`: its Java-compatible PCM conversion used at runtime.
- `audio/ambience/road-ambience.mp3`: the user-provided Lantern Street ambience.
- `audio/ambience/road-ambience.wav`: its Java-compatible runtime conversion, layered
  only while Alex is on Lantern Street.

All four packs are from Kenney and include their license files. The source pages list
the packs as Creative Commons CC0: `https://kenney.nl/assets/interface-sounds`,
`https://kenney.nl/assets/rpg-audio`, `https://kenney.nl/assets/ui-audio`, and
`https://kenney.nl/assets/music-jingles`.

The earlier OpenGameArt loop remains downloaded as a fallback reference, but the game
now uses the user-provided `solitude-main.wav` as its continuous ambient theme.
