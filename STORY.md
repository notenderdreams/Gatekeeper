# Gatekeeper Story and Progression

## Premise

Alex finds a mysterious box containing a notebook, a compact electronics workbench,
and unfamiliar electrical components. He does not initially know that they are logic
gates; he can only read the labels printed on them: `AND`, `OR`, and `NOT`. The box
contains five of each component.

The notebook begins with diagrams and truth tables, but later pages contain only truth
tables and incomplete device configurations. Unable to understand them, Alex takes the
components and notebook to Mira at the local electronics shop.

Mira identifies the components as logic gates and explains that larger devices can be
built from smaller ones. She offers Alex a deal: if he can construct useful components,
she will buy them. Alex returns home, installs the workbench, follows the notebook, and
manually tests his first circuits by changing every input combination and comparing the
results with the truth tables.

The story then grows into the main production loop: learn a design, prove it works,
certify it, manufacture it from owned parts, and sell it to fund more advanced work.

## Core Progression Rule

Every technology moves through four states:

```text
UNKNOWN -> COMMISSION UNLOCKED -> CRAFTED / UNVERIFIED -> CERTIFIED
```

- **Unknown:** Hidden from the shop, inventory, notebook, and tester.
- **Commission unlocked:** Mira requests the component. Its required
  behavior and truth table become visible, but it cannot yet be mass-produced.
- **Crafted / unverified:** Alex has assembled a circuit on the empty workbench and
  committed the physical parts to it. The exact player-made nodes and wiring are stored
  under `CRAFTED CIRCUITS` in the bag. This is not a purchased finished gate.
- **Certified:** Alex has built and successfully tested it once. The design can now be
  crafted repeatedly, used as an ingredient, sold, and requested by contracts.

Story progress introduces knowledge. Testing proves understanding. Certification turns
that understanding into production.

## Tester Philosophy

The LogicLens tester verifies a design; it does not solve it.

A tester configuration may reveal:

- The component name
- Input and output counts
- Expected truth table
- Individual test cases
- The row on which a circuit failed

It must not reveal the correct wiring. The player still has to design the circuit.

The first unit of every newly introduced design must be built from an empty canvas. Alex
can manually try the input switches at home, but before receiving the LogicLens this is
only his own inspection. Crafting the circuit consumes the actual components used and
stores the exact build in his bag. Mira performs the official four-row test when Alex
submits it at her counter. Once the LogicLens is unlocked, official verification can
happen at home. Certified later copies may use a faster production flow.

## Story Acts and Technology Tiers

### Tier 0: The Mystery Box

- Alex opens the box.
- He receives five components marked AND, five marked OR, and five marked NOT.
- He also receives the notebook and workbench.
- Alex does not yet know what the components are.
- The components appear in inventory, but unknown advanced components remain hidden.

### Tier 1: Fundamentals

- Alex brings the notebook and labeled components to Mira.
- Mira explains AND, OR, and NOT gates.
- She introduces NAND, NOR, and XOR configurations.
- Alex must build these circuits and manually record all four input combinations.
- The first contracts teach the buy, build, test, and sell loop.

### Tier 2: The LogicLens

- Alex returns after certifying NAND, NOR, and XOR.
- Mira recognizes his potential and gives him the LogicLens testing kit.
- The LogicLens automates complete truth-table verification.
- XNOR and IMPLY configurations become available.
- Advanced products remain more valuable because they require more components and more
  design work.

### Tier 3: Useful Devices

After the advanced gates are certified, the notebook begins revealing devices rather
than isolated gates:

- Half Adder
- Comparator
- SR Latch
- Multiplexer

These introduce multiple outputs, stored state, branching configurations, and components
made from previously certified products.

### Tier 4: Systems

Completing device contracts unlocks larger systems:

- Full Adder
- Multi-bit Comparator
- Alarm Controller
- Small memory and control assemblies

Tests now contain larger suites instead of a single four-row truth table.

### Tier 5: The Final Project

- Certifications gradually unlock the notebook's final pages.
- Alex and Mira learn why the box was sent to him.
- The final project combines logic, arithmetic, memory, and control branches.
- Completing its integrated test resolves the mystery of the sender and closes the main
  story while leaving repeatable contracts available.

## Technology Branches

```text
AND ----+
        +-- NAND ---------+
NOT ----+                  +-- SR LATCH
                           |
OR -----+                  |
        +-- NOR ----------+
NOT ----+

AND ----+
OR -----+-- XOR --+-- HALF ADDER -- FULL ADDER
NOT ----+         |
                  +-- XNOR -- COMPARATOR

NOT + OR -- IMPLY -- CONTROL CIRCUITS
```

The notebook acts as the visible technology tree. Branching pages represent logic,
arithmetic, memory, and control rather than introducing a separate generic skill-tree
screen.

## Production and Economy Loop

```text
Story introduces a configuration
-> player gathers or buys ingredients
-> player builds the first unit manually
-> tester verifies every required behavior
-> design becomes certified
-> player accepts production contracts
-> certified components are manufactured and sold
-> profits fund new ingredients, configurations, and upgrades
```

Crafting consumes its listed ingredients and produces exactly one finished component.
Finished components sell for more than the purchase cost of their ingredients, rewarding
the player's work. Buying a finished component directly costs more than its sale value,
preventing buy-and-resell exploits.

The economy must always provide a repeatable way to obtain primitive components so the
player cannot permanently lock progress by selling everything.

## First Playable Loop UI

- The notebook's `T` view is the visible design map. Unknown designs remain obscured,
  introduced designs show `TEST REQUIRED`, and certified designs show `PRODUCTION`.
- The workbench is a completely independent sandbox. It has no selected recipe, target
  name, expected truth table, or order text. `B` crafts whatever graph is currently on
  it, consuming exactly the gates the player placed.
- Crafting asks for a player-defined name/group. Later circuits may reuse an existing
  group or use a new one; the label never determines the circuit's behavior.
- The bag has separate `COMPONENTS` and `CRAFTED CIRCUITS` sections. A crafted circuit
  stores the player's exact graph. Select one for submission, or use `L` at the bench to
  unpack it, recover its committed parts, and edit it again.
- Mira's `C` order board includes NAND, NOR, and XOR from the first commission onward.
  The player selects an order and independently selects one crafted circuit to submit.
  Mira evaluates that stored graph against the order's truth table. Failed builds remain
  in the bag and report how many test cases failed; passing builds are accepted, paid
  for, and certified. There is no separate shop testing action.
- Mira's counter exposes `C` orders. Only certified designs receive orders; delivery
  removes finished items from inventory and pays a contract premium over ordinary sale
  value. Orders remain repeatable and their completed counts persist in the save.

## Narrative Through-Line

The mystery and mechanics should advance together:

```text
Story progress unlocks knowledge
-> knowledge unlocks puzzles
-> puzzles unlock production
-> production generates money
-> money funds deeper research
-> research reveals the next part of the mystery
```

Mira begins as a knowledgeable shopkeeper and becomes Alex's mentor and production
partner. Alex begins unable to identify the contents of the box and gradually becomes
capable of designing, certifying, and manufacturing complete electronic systems.
