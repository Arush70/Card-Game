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

    //Deck's ID number(Deck1, Deck2..).
    private final int id;

    //Collection of cards. The front of list is the top (drawn next by another player), tail is the bottom. 
    private final Queue<Card> cards;

    /**
     * Creates an empty deck and gives it ID number.
     *
     * @param id the deck number (deck 1, deck 2, ...)
     */
    public CardDeck(int id) {
        this.id = id;
        this.cards = new LinkedList<>();
    }

    /**
     * Checking which deck this is.
     *
     * @return the deck number
     */
    public int getId() {
        return id;
    }

    /**
     * Removes a card from the top of the deck.
     * If the deck is empty it will return 'null'.
     *
     * @return the top card, or {@code null} if the deck is empty
     */
    public synchronized Card draw() {
        return cards.poll();
    }

    /**
     * Puts a card to the bottom of the deck.
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
     * Returns the number of cards left in the deck.
     *
     * @return the current size
     */
    public synchronized int size() {
        return cards.size();
    }

    /**
     * Checks the deck and list out all the cards from top to bottom,
     * This are separated by spaces e.g. {@code "1 3 3 7"}.
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
     * Automatically creates a text file like "deck2_output.txt"
     * and writes exactly the cards left in it when the game was ended.
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
