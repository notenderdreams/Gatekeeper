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
