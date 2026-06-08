package card;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single player in the card game. Each player runs on their own thread.
 *
 * This class handles holding a 4-card hand, checking for a win (4 of a kind), 
 * and deciding which card to discard. The strategy is simple: keep cards that 
 * match the player's number, and discard any non-matching card at random.
 *
 * How thread safety works here:
 * A player's own hand and log are only touched by their own thread, so they 
 * don't need locks. The shared card decks are protected inside the CardDeck class 
 * using synchronized methods. The game-over flags use Atomic variables, which are 
 * safe to read and write across multiple threads without slow locking.
 *
 * To avoid deadlocks, a player takes a turn by interacting with one deck at a time 
 * (left deck first, then right deck). The game-over flag is only checked between 
 * turns, ensuring no thread stops halfway through a draw-and-discard action.
 */
public class Player implements Runnable {

	// Every player must hold exactly 4 cards.
    public static final int HAND_SIZE = 4;

    // player's ID number.
    private final int index;

    // The card value this player wants to keep, which matches their ID number.
    private final int preferred;

    // The four cards currently in the hand of player.
    private final List<Card> hand;

    // Random number generator to pick which unwanted card to discard.
    private final Random random;

    // The deck to the player's left that they draw from.
    private final CardDeck leftDeck;

    // The deck to the player's right that they discard into.
    private final CardDeck rightDeck;

    // Shared flag; true once any player has won. Set by the winner. 
    private final AtomicBoolean gameWon;

    // Shared flag that flips to true as soon as anyone wins the game.
    private final AtomicInteger winnerIndex;

    // A list of text lines tracking the player's actions, saved to a file at the end.
    private final List<String> log;

    /**
     * Standard constructor to set up a player in the game ring.
     *
     * @param index 1-based player index
     * @param initialHand the four cards this player starts with
     * @param leftDeck the deck this player draws from
     * @param rightDeck the deck this player discards to
     * @param gameWon shared flag, flipped to true once a player wins
     * @param winnerIndex shared winner identifier, set before {@code gameWon}
     */
    public Player(int index,
                  List<Card> initialHand,
                  CardDeck leftDeck,
                  CardDeck rightDeck,
                  AtomicBoolean gameWon,
                  AtomicInteger winnerIndex) {
        this(index, initialHand, leftDeck, rightDeck, gameWon, winnerIndex, new Random());
    }

    /**
     * Testing constructor that allows passing a specific Random object 
     * to make card discards predictable.
     */
    Player(int index,
           List<Card> initialHand,
           CardDeck leftDeck,
           CardDeck rightDeck,
           AtomicBoolean gameWon,
           AtomicInteger winnerIndex,
           Random random) {
        if (index <= 0) {
            throw new IllegalArgumentException(
                    "Player index must be positive, but was " + index);
        }
        if (initialHand == null || initialHand.size() != HAND_SIZE) {
            throw new IllegalArgumentException(
                    "Initial hand must contain exactly " + HAND_SIZE + " cards");
        }
        this.index = index;
        this.preferred = index;
        this.hand = new ArrayList<>(initialHand);
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.gameWon = gameWon;
        this.winnerIndex = winnerIndex;
        this.random = random;
        this.log = new ArrayList<>();
        log.add("player " + index + " initial hand " + handAsString());
    }

    /**
     * The main loop for the player thread. It checks for an immediate win, 
     * then keeps taking turns until someone else wins. Once the game ends, 
     * it logs the outcome and saves the file.
     */
    @Override
    public void run() {
        // Check if the player was dealt a winning hand right at the start.
        if (isWinningHand()) {
            declareWin();
            writeOutputFile();
            return;
        }
        while (!gameWon.get()) {
            takeTurn();
            if (isWinningHand()) {
                declareWin();
                break;
            }
        }
        if (winnerIndex.get() != index) {
            recordLossExit();
        }
        writeOutputFile();
    }

    /**
     * Handles drawing a card from the left deck and discarding one to the right deck. 
     * If the left deck is temporarily empty because the neighbor hasn't discarded yet, 
     * the player pauses briefly to let them catch up instead of burning CPU time.
     */
    void takeTurn() {
        Card drawn = leftDeck.draw();
        if (drawn == null) {
            // Another player hasn't discarded into our left deck yet; yield briefly.
            Thread.yield();
            return;
        }
        hand.add(drawn);
        Card toDiscard = chooseDiscard();
        hand.remove(toDiscard);
        rightDeck.discard(toDiscard);
        log.add("player " + index + " draws a " + drawn.getValue()
                + " from deck " + leftDeck.getId());
        log.add("player " + index + " discards a " + toDiscard.getValue()
                + " to deck " + rightDeck.getId());
        log.add("player " + index + " current hand is " + handAsString());
    }

    /**
     * Sets this player as the winner, updates the shared game state, 
     * and writes the win message to the console and log.
     */
    private void declareWin() {
        // Set the winner ID first, then flip the flag so other players see both updates together
        winnerIndex.set(index);
        gameWon.set(true);
        System.out.println("player " + index + " wins");
        log.add("player " + index + " wins");
        log.add("player " + index + " exits");
        log.add("player " + index + " final hand: " + handAsString());
    }

    /**
     * Adds the required loss message to the log when another player wins the game.
     */

    private void recordLossExit() {
        int winner = winnerIndex.get();
        log.add("player " + winner + " has informed player " + index
                + " that player " + winner + " has won");
        log.add("player " + index + " exits");
        log.add("player " + index + " hand: " + handAsString());
    }

    // Writes all tracked log actions to a text file named after the player.
    private void writeOutputFile() {
        String fileName = "player" + index + "_output.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String line : log) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("player " + index
                    + " could not write its output file: " + e.getMessage());
        }
    }


    // Returns the player's ID number.
    public int getIndex() { return index; }
    // Returns the card value this player wants to keep.
    public int getPreferred() { return preferred; }

    // Returns a safe copy of the hand so outside code can't accidentally change it.
    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    // Formats the hand into a clean space-separated string like "4 3 2 4".
    public String handAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(hand.get(i).getValue());
        }
        return sb.toString();
    }

    // Returns true if all cards in the hand have the exact same value.
    public boolean isWinningHand() {
        int first = hand.get(0).getValue();
        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).getValue() != first) return false;
        }
        return true;
    }

    /**
     * Returns a non-preferred card to discard, chosen uniformly at random.
     * Package-private tests in the same package (and {@link #takeTurn})
     * can invoke it.
     */
    Card chooseDiscard() {
        List<Integer> nonPreferred = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getValue() != preferred) {
                nonPreferred.add(i);
            }
        }
        if (nonPreferred.isEmpty()) {
            throw new IllegalStateException(
                    "Player " + index + " has no non-preferred cards to discard");
        }
        int pickIndex = nonPreferred.get(random.nextInt(nonPreferred.size()));
        return hand.get(pickIndex);
    }
}
