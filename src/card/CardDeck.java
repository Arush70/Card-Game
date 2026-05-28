package card;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.StringJoiner;

/**
 * A deck of cards arranged as a first-in, first-out (FIFO) queue.
 *
 * <p>Players draw a card from the top of the deck and discard a card to the
 * bottom, so the deck behaves like a queue: the card that has been waiting
 * longest is the next one drawn. Each deck is shared between two players (one
 * drawing from it, one discarding to it), so every method that reads or
 * changes the contents is {@code synchronized} on this deck. This guarantees
 * that a draw and a discard from different player threads can never interleave
 * and corrupt the queue.</p>
 */
public class CardDeck {

    /** The deck number, 1-based, used when naming the output file. */
    private final int id;

    /** The cards in the deck. The head is the top (drawn next), tail is the bottom. */
    private final Queue<Card> cards;

    /**
     * Creates an empty deck with the given 1-based identifier.
     *
     * @param id the deck number (deck 1, deck 2, ...)
     */
    public CardDeck(int id) {
        this.id = id;
        this.cards = new LinkedList<>();
    }

    /**
     * Returns this deck's 1-based identifier.
     *
     * @return the deck number
     */
    public int getId() {
        return id;
    }

    /**
     * Removes and returns the card at the top of the deck.
     *
     * @return the top card, or {@code null} if the deck is empty
     */
    public synchronized Card draw() {
        return cards.poll();
    }

    /**
     * Adds a card to the bottom of the deck.
     *
     * @param card the card to discard onto the bottom; must not be null
     */
    public synchronized void discard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot discard a null card");
        }
        cards.offer(card);
    }

    /**
     * Returns the number of cards currently in the deck.
     *
     * @return the current size
     */
    public synchronized int size() {
        return cards.size();
    }

    /**
     * Returns the deck's contents as space-separated values, top card first,
     * e.g. {@code "1 3 3 7"}.
     *
     * @return the space-separated card values
     */
    public synchronized String getContents() {
        StringJoiner joiner = new StringJoiner(" ");
        for (Card card : cards) {
            joiner.add(card.toString());
        }
        return joiner.toString();
    }

    /**
     * Writes this deck's final contents to {@code deck<id>_output.txt} in the
     * form required by the specification, e.g. {@code "deck2 contents: 1 3 3 7"}.
     *
     * @throws IOException if the file cannot be written
     */
    public synchronized void writeOutputFile() throws IOException {
        String fileName = "deck" + id + "_output.txt";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("deck" + id + " contents: " + getContents());
        }
    }
}
