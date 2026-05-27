package card;

/**
 * Represents a single playing card with a fixed face value (denomination).
 *
 * <p>A {@code Card} is immutable: its value is set once at construction and can
 * never change. Because the only field is {@code final} and there are no
 * mutating methods, instances are inherently thread-safe and can be shared
 * freely between player threads and decks without synchronization.</p>
 */
public final class Card {

    /** The face value of this card. Non-negative and never changes. */
    private final int value;

    /**
     * Creates a card with the given face value.
     *
     * @param value the face value; must be zero or greater
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public Card(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Card value must be non-negative, but was " + value);
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}