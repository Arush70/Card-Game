package card;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link CardDeck} class.
 *
 * <p>These tests verify the first-in, first-out behaviour (draw from the top,
 * discard to the bottom), the size and contents reporting, and the handling of
 * boundary cases such as drawing from an empty deck and discarding null.</p>
 */
class CardDeckTest {

    private CardDeck deck;

    @BeforeEach
    void setUp() {
        deck = new CardDeck(1);
    }

    @Test
    @DisplayName("A new deck is empty")
    void newDeckIsEmpty() {
        assertEquals(0, deck.size());
    }

    @Test
    @DisplayName("Drawing from an empty deck returns null")
    void drawFromEmptyReturnsNull() {
        assertNull(deck.draw());
    }

    @Test
    @DisplayName("Discard then draw returns the same card")
    void discardThenDraw() {
        Card c = new Card(5);
        deck.discard(c);
        assertEquals(5, deck.draw().getValue());
    }

    @Test
    @DisplayName("Cards are drawn in FIFO order: top is the oldest discarded")
    void drawsInFifoOrder() {
        deck.discard(new Card(1));
        deck.discard(new Card(2));
        deck.discard(new Card(3));
        assertEquals(1, deck.draw().getValue());
        assertEquals(2, deck.draw().getValue());
        assertEquals(3, deck.draw().getValue());
    }

    @Test
    @DisplayName("size reflects the number of cards held")
    void sizeReflectsContents() {
        deck.discard(new Card(7));
        deck.discard(new Card(8));
        assertEquals(2, deck.size());
        deck.draw();
        assertEquals(1, deck.size());
    }

    @Test
    @DisplayName("getContents lists values top-first, space separated")
    void contentsAreSpaceSeparatedTopFirst() {
        deck.discard(new Card(1));
        deck.discard(new Card(3));
        deck.discard(new Card(3));
        deck.discard(new Card(7));
        assertEquals("1 3 3 7", deck.getContents());
    }

    @Test
    @DisplayName("getId returns the deck's identifier")
    void getIdReturnsId() {
        assertEquals(1, deck.getId());
    }

    @Test
    @DisplayName("Discarding null is rejected")
    void discardNullRejected() {
        assertThrows(IllegalArgumentException.class, () -> deck.discard(null));
    }
}
