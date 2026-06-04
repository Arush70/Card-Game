package card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Player}.
 *
 * <p>These tests verify hand storage and reporting, the winning-hand check
 * (including non-preferred winners), the discard strategy (never the
 * preferred denomination; random choice among the rest), and constructor
 * validation. One test uses Java Reflection to invoke the package-private
 * {@code chooseDiscard()} method &mdash; the specification's Q&amp;A
 * explicitly invites Reflection-based testing of non-public methods.</p>
 */
class PlayerTest {

    /** Shorthand to construct a hand of four cards from four ints. */
    private static List<Card> hand(int a, int b, int c, int d) {
        return new ArrayList<>(Arrays.asList(
                new Card(a), new Card(b), new Card(c), new Card(d)));
    }

    @Test
    @DisplayName("Constructor stores index, preferred denomination, and hand")
    void constructorStoresFields() {
        Player p = new Player(2, hand(1, 2, 3, 4));
        assertEquals(2, p.getIndex());
        assertEquals(2, p.getPreferred());
        assertEquals(4, p.getHand().size());
    }

    @Test
    @DisplayName("getHand returns a defensive copy, not the live hand")
    void getHandIsDefensiveCopy() {
        Player p = new Player(1, hand(1, 1, 1, 1));
        List<Card> snapshot = p.getHand();
        snapshot.clear();
        assertEquals(4, p.getHand().size(),
                "Clearing the returned list must not affect the player");
    }

    @Test
    @DisplayName("handAsString renders space-separated values in order")
    void handAsStringFormat() {
        Player p = new Player(1, hand(1, 1, 2, 4));
        assertEquals("1 1 2 4", p.handAsString());
    }

    @Test
    @DisplayName("Four of the preferred denomination is a winning hand")
    void preferredFourOfAKindWins() {
        Player p = new Player(1, hand(1, 1, 1, 1));
        assertTrue(p.isWinningHand());
    }

    @Test
    @DisplayName("Four of a NON-preferred denomination is still a winning hand")
    void nonPreferredFourOfAKindStillWins() {
        Player p = new Player(1, hand(5, 5, 5, 5));
        assertTrue(p.isWinningHand());
    }

    @Test
    @DisplayName("A mixed hand is not a winning hand")
    void mixedHandDoesNotWin() {
        Player p = new Player(1, hand(1, 1, 2, 1));
        assertFalse(p.isWinningHand());
    }

    @Test
    @DisplayName("chooseDiscard NEVER returns a preferred card")
    @RepeatedTest(50)
    void discardIsNeverPreferred() {
        // Player 3 prefers 3s; hand contains three 3s and one 9.
        Player p = new Player(3, hand(3, 3, 3, 9));
        Card discard = p.chooseDiscard();
        assertNotEquals(3, discard.getValue(),
                "chooseDiscard returned a preferred card, breaking the rule");
        assertEquals(9, discard.getValue(),
                "Only 9 is non-preferred, so it must be the chosen card");
    }

    @Test
    @DisplayName("chooseDiscard picks among ALL non-preferred cards over many trials")
    void discardCoversAllNonPreferred() {
        // Player 1 prefers 1s; three non-preferred values: 2, 3, 4.
        Player p = new Player(1, hand(1, 2, 3, 4));
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(p.chooseDiscard().getValue());
        }
        assertTrue(seen.contains(2) && seen.contains(3) && seen.contains(4),
                "Over 200 trials, every non-preferred value should appear; saw " + seen);
        assertFalse(seen.contains(1),
                "The preferred value 1 must never appear");
    }

    @Test
    @DisplayName("chooseDiscard with a seeded Random is deterministic")
    void deterministicWithSeededRandom() {
        // Same seed -> same choice each time
        Player a = new Player(1, hand(1, 2, 3, 4), new Random(42));
        Player b = new Player(1, hand(1, 2, 3, 4), new Random(42));
        assertEquals(a.chooseDiscard().getValue(), b.chooseDiscard().getValue());
    }

    @Test
    @DisplayName("chooseDiscard throws if there are no non-preferred cards (already winning)")
    void chooseDiscardThrowsWhenAllPreferred() {
        Player p = new Player(2, hand(2, 2, 2, 2));
        assertThrows(IllegalStateException.class, p::chooseDiscard);
    }

    @Test
    @DisplayName("Constructor rejects an invalid hand or non-positive index")
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(0, hand(1, 2, 3, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Player(1, new ArrayList<>())); // wrong size
    }

    @Test
    @DisplayName("addToHand / removeFromHand round-trip preserves hand size")
    void addAndRemoveRoundTrip() {
        Player p = new Player(1, hand(1, 2, 3, 4));
        Card extra = new Card(7);
        p.addToHand(extra);
        assertEquals(5, p.getHand().size());
        assertTrue(p.removeFromHand(extra));
        assertEquals(4, p.getHand().size());
    }

    /**
     * Java Reflection demonstration as invited by the brief: invoke the
     * package-private {@code chooseDiscard()} via Reflection rather than
     * directly. This proves the technique works, which we will reuse to test
     * any genuinely private helpers added later.
     */
    @Test
    @DisplayName("Java Reflection can invoke chooseDiscard() on a Player")
    void invokeChooseDiscardByReflection() throws Exception {
        Player p = new Player(2, hand(2, 2, 2, 9));
        Method m = Player.class.getDeclaredMethod("chooseDiscard");
        m.setAccessible(true);
        Object result = m.invoke(p);
        assertNotNull(result);
        assertTrue(result instanceof Card);
        assertEquals(9, ((Card) result).getValue(),
                "Even when invoked reflectively, the non-preferred card is chosen");
    }
}
