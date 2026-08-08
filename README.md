# GATEKEEPER: A Logic Tale

A small Java 2D story/puzzle game based on `STORY.md`. It uses Java2D for a crisp low-resolution retro-RPG presentation, original illustrated three-quarter-view environments, pixel-style characters, monochrome dialogue boxes, and an interactive circuit crafting board. Gradle provides the build and run lifecycle; the project has no runtime dependencies.

The original environment art, animated character sheets, and generation notes are
documented in `ASSETS.md`.

## Play

Requires Java 17 or newer, Gradle, and optionally [just](https://github.com/casey/just) for shortcuts.

```sh
just run
```

The equivalent Gradle commands are `gradle run`, `gradle build`, and
`gradle logicTest`. If `just` is installed, run `just` to list the shortcuts.

## Controls

- `WASD` / arrow keys — move Alex
- `E` / `Enter` — interact or continue dialogue
- `N` — open or close the notebook, including while using the crafting board
- Left/right arrows — browse unlocked project pages in the notebook
- Mouse — operate the crafting board
- `1`, `2`, `3` — select AND, OR, or NOT on the board
- `A`, `B` — toggle the two board inputs
- `R` — record the current truth-table row
- `T` / `Enter` — verify or run the tester
- `Esc` — leave the board or notebook

## Game loop

1. Explore Alex's room and find the box of basic logic gates.
2. Leave home, walk along the side-view Lantern Street, and enter Mira's electronics shop.
3. Build NAND, NOR, and XOR from AND, OR, and NOT. The board uses real branching
   circuit topology—for example, XOR builds `A AND NOT B` and `NOT A AND B` in
   parallel before combining them with OR.
4. Manually try all four switch combinations and record the output.
5. Deliver the circuits to unlock the LogicLens automatic tester.
6. Complete XNOR and IMPLY to finish the prototype story.

Run the dependency-free logic checks with:

```sh
just test
# or: gradle logicTest
```
