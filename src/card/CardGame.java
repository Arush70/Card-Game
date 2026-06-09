package card;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;

/**
 * The executable entry point for the multi-threaded card game simulation.
 *
 * This class acts as the central orchestrator. It does not contain any game-rule logic
 * itself; instead, it composes the existing components by prompting the user,
 * delegating pack validation to PackReader, dealing cards via Dealer,
 * starting the Player threads, awaiting completion, and writing each
 * CardDeck's final output file. Keeping CardGame as pure orchestration ensures 
 * a deliberate separation of concerns where each class has exactly one job.
 *
 * Input is collected via JOptionPane dialogs where a display is available, 
 * and falls back automatically to the terminal in a headless environment, 
 * ensuring the game runs identically in both situations. The exact same 
 * validation logic is applied across both input paths.
 */
public class CardGame {

    // Standard output stream, made overridable for tests if ever needed.
    private static final PrintStream OUT = System.out;

    // Whether to use graphical dialogs (true) or terminal prompts (false). 
    private final boolean useDialog;

    // Scanner over standard input, only created when the terminal is used. 
    private final Scanner scanner;

    /**
     * Default constructor: chooses dialog vs terminal automatically based on
     * whether a display is available.
     */
    public CardGame() {
        this(!GraphicsEnvironment.isHeadless());
    }

    /**
     * Test-friendly constructor that lets callers force the terminal path.
     *
     * @param useDialog true to use JOptionPane, false to use the terminal
     */
    public CardGame(boolean useDialog) {
        this.useDialog = useDialog;
        this.scanner = useDialog ? null : new Scanner(System.in);
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        new CardGame().play();
    }

    /**
     * Runs one full game loop: prompts for inputs, validates them, deals the deck, 
     * spawns the concurrent threads, blocks until execution completes, and writes 
     * the final deck state files.
     */
    public void play() {
        int n = promptForNumberOfPlayers();
        List<Card> pack = promptForValidPack(n);

        // Deal and fill. From this point on the layout is fixed.
        List<List<Card>> hands = Dealer.dealHands(pack, n);
        List<CardDeck> decks = Dealer.fillDecks(pack, n);

        // Shared signals: which player won, and a stop flag the others observe.
        AtomicBoolean gameWon = new AtomicBoolean(false);
        AtomicInteger winnerIndex = new AtomicInteger(0);

        // Build the players around the ring of decks.
        Player[] players = new Player[n];
        for (int i = 0; i < n; i++) {
            CardDeck left = decks.get(i);
            CardDeck right = decks.get((i + 1) % n);
            players[i] = new Player(i + 1, hands.get(i), left, right,
                    gameWon, winnerIndex);
        }

        // Start the threads and wait for them all to finish.
        Thread[] threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            threads[i] = new Thread(players[i], "Player-" + (i + 1));
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                OUT.println("Interrupted while waiting for players to finish");
                return;
            }
        }

        // Write the deck output files now that all players have stopped.
        for (CardDeck deck : decks) {
            try {
                deck.writeOutputFile();
            } catch (IOException e) {
                OUT.println("Could not write deck"
                        + deck.getId() + "_output.txt: " + e.getMessage());
            }
        }

        OUT.println("Output files written: "
                + (2 * n) + " files (" + n + " player files, "
                + n + " deck files)");
    }

    // Input prompts

     /**
     * Prompts the user for number of players, iterating until a positive integer 
     * is successfully parsed.
     */
    int promptForNumberOfPlayers() {
        while (true) {
            String raw = ask("Please enter the number of players:");
            if (raw == null) {
                // User cancelled the dialog: fall back to terminal.
                continue;
            }
            try {
                int n = Integer.parseInt(raw.trim());
                if (n <= 0) {
                    showError("Number of players must be a positive integer.");
                    continue;
                }
                return n;
            } catch (NumberFormatException e) {
                showError("'" + raw + "' is not a valid number. Please try again.");
            }
        }
    }

    /**
     * Prompts the user for a pack file path, re-prompting until a valid pack
     * for the given {@code n} is supplied. The same validation is applied
     * whether input arrives from the terminal or a dialog.
     */
    List<Card> promptForValidPack(int n) {
        while (true) {
            String path = ask("Please enter location of pack to load:");
            if (path == null) continue;
            try {
                return PackReader.readAndValidate(path.trim(), n);
            } catch (IOException e) {
                showError("Could not read pack file: " + e.getMessage());
            } catch (InvalidPackException e) {
                showError("Invalid pack: " + e.getMessage());
            }
        }
    }

    // Sends a prompt to the user and returns the response, or null on cancel. 
    private String ask(String prompt) {
        if (useDialog) {
            return JOptionPane.showInputDialog(null, prompt);
        }
        OUT.print(prompt + "\n");
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    // Shows an error message via dialog or terminal. 
    private void showError(String message) {
        if (useDialog) {
            JOptionPane.showMessageDialog(null, message,
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            OUT.println(message);
        }
    }
}
