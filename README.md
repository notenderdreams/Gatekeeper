# GATEKEEPER: A Logic Tale

A small, dependency-free Java 2D story/puzzle game based on `STORY.md`. It uses Java2D for a crisp low-resolution retro-RPG presentation, original illustrated three-quarter-view environments, pixel-style characters, monochrome dialogue boxes, and an interactive circuit crafting board.

The original environment art, animated character sheets, and generation notes are
documented in `ASSETS.md`.

## Play

Requires Java 17 or newer.

```sh
./run.sh
```

If the scripts are not executable yet, run `chmod +x run.sh test.sh` once, or use `sh run.sh`.

## Controls

- `WASD` / arrow keys — move Alex
- `E` / `Enter` — interact or continue dialogue
- `N` — open or close the notebook after finding it
- Mouse — operate the crafting board
- `1`, `2`, `3` — select AND, OR, or NOT on the board
- `A`, `B` — toggle the two board inputs
- `R` — record the current truth-table row
- `T` / `Enter` — verify or run the tester
- `Esc` — leave the board or notebook

## Game loop

1. Explore Alex's room and find the box of basic logic gates.
2. Take the notebook to Mira at the electronics shop.
3. Build NAND, NOR, and XOR from AND, OR, and NOT. The board uses real branching
   circuit topology—for example, XOR runs OR and AND in parallel before combining them.
4. Manually try all four switch combinations and record the output.
5. Deliver the circuits to unlock the LogicLens automatic tester.
6. Complete XNOR and IMPLY to finish the prototype story.

Run the dependency-free logic checks with:

```sh
./test.sh
```
