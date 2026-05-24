# User Stories — COMM110J Card Game Simulation

---

## Must have

### US1 — Specify number of players
As a user, I want to enter the number of players `n`, so that the correct number of
players and decks is created.
- **Accept:** a positive integer is accepted; zero, a negative number, or non-numeric
  input is rejected and the user is prompted again.

### US2 — Provide a pack file
As a user, I want to give the location of a pack file, so that the cards to be dealt are
loaded.
- **Accept:** a readable file path loads successfully; a missing or unreadable file is
  reported and the user is prompted again.

### US3 — Reject invalid packs
As a user, I want an invalid pack rejected, so that the game starts only with valid input.
- **Accept:** a pack is valid only if it has exactly `8n` rows and every row is a single
  non-negative integer; any other pack is rejected with a message and a re-prompt; the
  game does not start until both `n` and the pack are valid.

### US4 — Correct initial distribution
As a user, I want hands dealt and decks filled in round-robin order, so that the starting
state matches the specification.
- **Accept:** the first `4n` cards are dealt one at a time to players 1..n repeatedly
  until each holds four; the remaining `4n` cards fill decks 1..n the same way.

### US5 — Concurrent play
As a user, I want every player to run as its own thread, so that play is simultaneous,
not sequential.
- **Accept:** `n` player threads run concurrently; no player waits for another to finish
  a full turn before starting its own.

### US6 — Win on four of a kind
As a user, I want a player holding four equal cards to win and end the game, so that the
game terminates correctly.
- **Accept:** a player with four cards of the same value (preferred or not) declares a
  win; on a win, all players stop; exactly one win is declared per game.

### US7 — Announce winner on screen
As a user, I want the winner shown on the terminal as `player i wins`, so that I see the
result immediately.
- **Accept:** the winning player's index is printed to standard output, both for an
  immediate win and a win during play.

### US8 — Produce output files
As a user, I want `2n` output files in the exact specified format, so that the game is
fully recorded.
- **Accept:** each `playerN_output.txt` opens with the initial hand and ends with the
  win/exit lines and final hand; each `deckN_output.txt` contains one line of its final
  contents; formats match the brief exactly.

### US13 — Dialog-based input
As a user, I want to enter the number of players and pack location through JOptionPane
dialog boxes, so that I can configure the game graphically as well as from the terminal.
- **Accept:** both inputs can be supplied via dialog; the same validation (US1, US3,
  US12) applies to dialog input as to terminal input; in a headless environment with no
  display, the program falls back to terminal input automatically.

---

## Should have

### US9 — Follow the discard strategy
As a user, I want each player to keep its preferred denomination and discard a
non-preferred card, so that play follows the rules.
- **Accept:** a player never discards a card equal to its index; if more than one
  non-preferred card is held, one is chosen at random to discard.

### US10 — No stagnation
As a user, I want a player never to hold a non-preferred card indefinitely, so that the
game cannot stall.
- **Accept:** across turns, non-preferred cards remain eligible for discard so that no
  card is retained permanently.

### US11 — Atomic draw and discard
As a user, I want each draw-and-discard treated as one atomic action, so that every
player ends with four cards.
- **Accept:** a player's hand size returns to four before any observer can act; at game
  end every player holds exactly four cards.

### US12 — Clear file errors
As a user, I want a clear message when a pack cannot be found or read, so that I can
correct the path.
- **Accept:** file-not-found and read errors are reported distinctly from validity
  errors, and the user is re-prompted.

---

## Could have

### US14 — Output confirmation
As a user, I want a confirmation that output files were written, so that I know where to
find them.
- **Accept:** after the game ends, the system indicates that player and deck files have
  been written.

---

## Won't have (this iteration)

### US15 — Simultaneous wins
As a user, I will not expect the system to resolve two players winning in the same
instant; per the brief this rare case is handled by re-running.

### US16 — Optimal strategy
As a user, I will not expect optimal play; the specified strategy is deliberately simple.
