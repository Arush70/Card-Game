package card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Card} class.
 *
 * <p>These tests verify that a card stores the value it is given, reports it
 * correctly through {@code getValue()} and {@code toString()}, accepts the
 * boundary value zero, and rejects negative values as required by the
 * specification (a card value must be a non-negative integer).</p>
 */
class CardTest {

    @Test
    @DisplayName("getValue returns the value the card was created with")
    void getValueReturnsConstructedValue() {
        Card card = new Card(7);
        assertEquals(7, card.getValue());
    }

    @Test
    @DisplayName("A card with value zero is allowed (zero is non-negative)")
    void zeroIsAValidValue() {
        Card card = new Card(0);
        assertEquals(0, card.getValue());
    }

    @Test
    @DisplayName("A large value above n is allowed (the spec permits this)")
    void largeValueIsAllowed() {
        Card card = new Card(99);
        assertEquals(99, card.getValue());
    }

    @Test
    @DisplayName("toString renders the card as its numeric value")
    void toStringIsTheNumber() {
        Card card = new Card(4);
        assertEquals("4", card.toString());
    }

    @Test
    @DisplayName("Constructing a card with a negative value throws")
    void negativeValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }
}