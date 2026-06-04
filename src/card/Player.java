package card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A single player in the card game.
 *
 * <p>This class encapsulates the player's logic: holding a hand of four cards,
 * checking whether the hand is a winning hand (four cards of the same value),
 * and choosing which card to discard under the specified strategy (keep cards
 * of the preferred denomination, discard a non-preferred card at random).</p>
 *
 * <p>Threading is intentionally not added in this class yet. The fields are
 * kept private and access is funnelled through methods so that when the
 * threading is added on Day 8 (an implementation of {@link Runnable}),
 * synchronisation can be applied in one place without rewriting the logic.
 * For now, all behaviour is single-threaded and pure to make it easy to
 * unit-test in isolation.</p>
 */
public class Player {

    /** Hand size required by the specification. */
    public static final int HAND_SIZE = 4;

    /** 1-based player index. Players are labelled 1..n. */
    private final int index;

    /** This player's preferred card denomination, equal to {@link #index}. */
    private final int preferred;

    /** The four cards currently in the hand. */
    private final List<Card> hand;

    /** Source of randomness for choosing which non-preferred card to discard. */
    private final Random random;

    /**
     * Creates a player with the given 1-based index and an initial hand.
     *
     * @param index 1-based player index; must be positive
     * @param initialHand the hand to start with; must contain exactly
     *                    {@link #HAND_SIZE} cards
     * @throws IllegalArgumentException if either argument is invalid
     */
    public Player(int index, List<Card> initialHand) {
        this(index, initialHand, new Random());
    }

    /**
     * Constructor used by tests to inject a deterministic {@link Random},
     * so that the random-discard behaviour can be tested predictably.
     *
     * @param index 1-based player index
     * @param initialHand the starting hand
     * @param random the random source to use for discard selection
     */
    Player(int index, List<Card> initialHand, Random random) {
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
        this.random = random;
    }

    /**
     * Returns this player's 1-based index.
     *
     * @return the player number
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns this player's preferred card denomination (equal to the index).
     *
     * @return the preferred denomination
     */
    public int getPreferred() {
        return preferred;
    }

    /**
     * Returns a defensive copy of the current hand, so external code can read
     * it (for logging, testing, deck output) without being able to modify the
     * player's real hand.
     *
     * @return a snapshot of the hand
     */
    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    /**
     * Renders the hand as space-separated values in current order,
     * e.g. {@code "1 1 2 4"}, matching the format required for output files.
     *
     * @return the hand as a space-separated string
     */
    public String handAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(hand.get(i).getValue());
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} if every card in the hand has the same value.
     *
     * <p>The specification states that a player with four cards of the same
     * value wins, regardless of whether that value is the player's preferred
     * denomination.</p>
     *
     * @return whether this hand is a winning hand
     */
    public boolean isWinningHand() {
        int first = hand.get(0).getValue();
        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).getValue() != first) return false;
        }
        return true;
    }

    /**
     * Chooses a card to discard from the current hand according to the
     * specified strategy: never discard a card of the preferred denomination;
     * if more than one non-preferred card is held, choose one at random.
     *
     * <p>This method assumes the hand is not already a winning hand of the
     * preferred denomination &mdash; if every card matches the preferred
     * value, the player has already won and should not be discarding. If the
     * hand somehow contains no non-preferred cards at all, an
     * {@link IllegalStateException} is thrown to surface the bug rather than
     * silently violating the discard rule.</p>
     *
     * @return the card to discard (not yet removed from the hand)
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

    /**
     * Adds a drawn card to the hand. This is exposed for the future
     * {@code run()} loop and for tests.
     *
     * @param card the card just drawn; must not be null
     */
    void addToHand(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot add a null card to the hand");
        }
        hand.add(card);
    }

    /**
     * Removes a specific card object from the hand. Used after a discard has
     * been chosen by {@link #chooseDiscard()}.
     *
     * @param card the card to remove (compared by identity, not value)
     * @return whether the card was found and removed
     */
    boolean removeFromHand(Card card) {
        return hand.remove(card);
    }
}
