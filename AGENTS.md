# Gatekeeper agent instructions

## Project workflow

- This is a Java 17 game built with Gradle.
- Run the focused checks with `./test.sh`.
- Compile and run the game with `gradle lwjgl3:run --no-daemon --console=plain` when a visual verification is needed.
- Preserve the existing pixel-art presentation and keep rendering components outside `GamePanel`; the panel should coordinate state, input, updates, and renderer assembly.

## Codebase context with Graphify

- Graphify is the preferred starting point for architecture, dependency, and change-impact investigations.
- Read the existing local graph at `graphify-out/graph.json` before broad source searches.
- Use the graph to find the module that already owns a responsibility before adding code. Extend that module or create a focused component beside it; do not place unrelated behavior in whichever file is easiest to reach.
- Keep classes cohesive and reasonably sized. Split scenes, reusable drawing components, game-state logic, input handling, and asset loading into separate modules instead of building catch-all files.
- Treat `GamePanel` as an assembly boundary: it may coordinate state, updates, input, and renderer selection, but screen painting and reusable visual components belong in dedicated renderer/component classes.
- Before introducing a new class or moving code, use `graphify query`, `graphify path`, and `graphify affected` to check ownership, dependencies, and downstream impact. Prefer a module with a narrow purpose and clear dependency direction.
- After a structural change, query the affected symbols again to confirm the new module boundaries are represented correctly and that no accidental dependency cycle or oversized central class was introduced.
- Ask targeted architecture questions with `graphify query "<question>"`.
- Inspect symbol relationships with `graphify path "<node A>" "<node B>"`.
- Explain a symbol and its immediate neighbors with `graphify explain "<node>"`.
- Check downstream impact before structural changes with `graphify affected "<node>"`.
- Refresh the graph after adding, removing, renaming, or moving code with `graphify update . --no-cluster`.
- Graphify output is generated local context and is intentionally excluded from Git via `graphify-out/` in `.gitignore`.
- If the graph is missing, stale, or cannot answer a question, fall back to focused `rg` searches and source inspection, then refresh it.
