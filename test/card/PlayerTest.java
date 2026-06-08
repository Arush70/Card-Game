package card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Player}, including end-to-end concurrent runs.
 *
 * <p>The single-thread tests check the game rules on their own. The multi-threaded 
 * test sets up 4 players around 4 decks, lets them play the game all at once, 
 * and makes sure the game finishes with only 1 winner and everyone still 
 * holding exactly 4 cards.
 */
class PlayerTest {

    //Quick helper to create a 4-card hand using 4 numbers.
    private static List<Card> hand(int a, int b, int c, int d) {
        return new ArrayList<>(Arrays.asList(
                new Card(a), new Card(b), new Card(c), new Card(d)));
    }

    // Setting up a single player with fake decks and game flags for basic testing.
    private static Player isolatedPlayer(int idx, List<Card> h) {
        return new Player(idx, h, new CardDeck(idx), new CardDeck(idx + 1),
                new AtomicBoolean(false), new AtomicInteger(0));
    }

    //Same as above, but allows passing a set random seed to make tests predictable.
    private static Player isolatedPlayer(int idx, List<Card> h, Random rng) {
        return new Player(idx, h, new CardDeck(idx), new CardDeck(idx + 1),
                new AtomicBoolean(false), new AtomicInteger(0), rng);
    }

    @Test
    @DisplayName("Constructor stores index, preferred denomination, hand")
    void constructorStoresFields() {
        Player p = isolatedPlayer(2, hand(1, 2, 3, 4));
        assertEquals(2, p.getIndex());
        assertEquals(2, p.getPreferred());
        assertEquals(4, p.getHand().size());
    }

    @Test
    @DisplayName("getHand returns a defensive copy")
    void getHandIsDefensiveCopy() {
        Player p = isolatedPlayer(1, hand(1, 1, 1, 1));
        p.getHand().clear();  // Clearing the returned list shouldn't break the actual hand
        assertEquals(4, p.getHand().size());
    }

    @Test
    @DisplayName("handAsString renders space-separated values")
    void handAsStringFormat() {
        assertEquals("1 1 2 4",
                isolatedPlayer(1, hand(1, 1, 2, 4)).handAsString());
    }

    @Test
    @DisplayName("Four of preferred wins")
    void preferredFourOfAKindWins() {
        assertTrue(isolatedPlayer(1, hand(1, 1, 1, 1)).isWinningHand());
    }

    @Test
    @DisplayName("Four of non-preferred still wins")
    void nonPreferredFourOfAKindStillWins() {
        assertTrue(isolatedPlayer(1, hand(5, 5, 5, 5)).isWinningHand());
    }

    @Test
    @DisplayName("A mixed hand is not a winning hand")
    void mixedHandDoesNotWin() {
        assertFalse(isolatedPlayer(1, hand(1, 1, 2, 1)).isWinningHand());
    }

    @Test
    @DisplayName("chooseDiscard never returns a preferred card")
    @RepeatedTest(50)  // Runs 50 times to make absolutely sure it never picks the favorite card
    void discardIsNeverPreferred() {
        Player p = isolatedPlayer(3, hand(3, 3, 3, 9));
        Card d = p.chooseDiscard();
        assertNotEquals(3, d.getValue());
        assertEquals(9, d.getValue());
    }

    @Test
    @DisplayName("Over many trials, every non-preferred value is reachable")
    void discardCoversAllNonPreferred() {
        Player p = isolatedPlayer(1, hand(1, 2, 3, 4));
        Set<Integer> seen = new HashSet<>();
     // Discard 200 times to verify it randomly cycles through all unwanted cards
        for (int i = 0; i < 200; i++) seen.add(p.chooseDiscard().getValue());
        assertTrue(seen.contains(2) && seen.contains(3) && seen.contains(4));
        assertFalse(seen.contains(1));  // Should never throw away the favorite card

    }

    @Test
    @DisplayName("Seeded Random makes chooseDiscard deterministic")
    void deterministicWithSeededRandom() {
    	// Two players with the exact same seed should throw away the exact same card
        Player a = isolatedPlayer(1, hand(1, 2, 3, 4), new Random(42));
        Player b = isolatedPlayer(1, hand(1, 2, 3, 4), new Random(42));
        assertEquals(a.chooseDiscard().getValue(), b.chooseDiscard().getValue());
    }

    @Test
    @DisplayName("chooseDiscard throws if every card is preferred")
    void chooseDiscardThrowsWhenAllPreferred() {
        Player p = isolatedPlayer(2, hand(2, 2, 2, 2));
        assertThrows(IllegalStateException.class, p::chooseDiscard);
    }

    @Test
    @DisplayName("Constructor rejects invalid arguments")
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> isolatedPlayer(0, hand(1, 2, 3, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> isolatedPlayer(1, null));
        assertThrows(IllegalArgumentException.class,
                () -> isolatedPlayer(1, new ArrayList<>()));
    }

    @Test
    @DisplayName("Reflection can invoke the package-private chooseDiscard")
    void invokeChooseDiscardByReflection() throws Exception {
        Player p = isolatedPlayer(2, hand(2, 2, 2, 9));
        Method m = Player.class.getDeclaredMethod("chooseDiscard");
        m.setAccessible(true);
        Object r = m.invoke(p);
        assertNotNull(r);
        assertTrue(r instanceof Card);
        assertEquals(9, ((Card) r).getValue());
    }


    @Test
    @DisplayName("End-to-end: four threads run, one winner, all players hold 4 cards")
    void endToEndConcurrentRun() throws Exception {
        // Build a pack where ONLY player 1 starts with four of a kind.
        // Round-robin dealing means player k receives pack positions k-1, k+3, k+7, k+11.
        // We force positions 0,4,8,12 to all be value 1 and ensure the other groups
        // are mixed (so no other player has an immediate winning hand).
        int[] vals = new int[32];
        for (int i = 0; i < 32; i++) {
            vals[i] = (i % 4 == 0) ? 1 : 0;
        }
        vals[1] = 7;  vals[5]  = 11; vals[9]  = 12; vals[13] = 13; // player 2 hand: 7,11,12,13
        vals[2] = 5;  vals[6]  = 14; vals[10] = 15; vals[14] = 16; // player 3 hand: 5,14,15,16
        vals[3] = 9;  vals[7]  = 17; vals[11] = 18; vals[15] = 19; // player 4 hand: 9,17,18,19
        List<Card> pack = new ArrayList<>();
        for (int v : vals) pack.add(new Card(v));

        // Use the dealer to give out the hands and load the decks
        List<List<Card>> hands = Dealer.dealHands(pack, 4);
        List<CardDeck> decks = Dealer.fillDecks(pack, 4);

        AtomicBoolean gameWon = new AtomicBoolean(false);
        AtomicInteger winnerIndex = new AtomicInteger(0);

        Player[] players = new Player[4];
        for (int i = 0; i < 4; i++) {
            CardDeck left = decks.get(i);
            CardDeck right = decks.get((i + 1) % 4);
            players[i] = new Player(i + 1, hands.get(i), left, right,
                    gameWon, winnerIndex);
        }

        // Start all 4 player threads at the same time
        Thread[] threads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(players[i], "Player-" + (i + 1));
            threads[i].start();
        }
        
        // Wait up to 5 seconds for everyone to finish playing
        for (Thread t : threads) t.join(5000);
        for (Thread t : threads) assertFalse(t.isAlive(), "A thread did not finish");

        // Double check that the game successfully marked a winner
        assertTrue(gameWon.get(), "gameWon must be set at end");
        assertEquals(1, winnerIndex.get(), "Player 1 had the immediate winning hand");
        
        // Making sure no player dropped or gained extra cards during the multi-threaded action
        for (Player p : players) {
            assertEquals(4, p.getHand().size(),
                    "Every player must end holding 4 cards");
        }

        // Clean up the output files this test produced in the working directory.
        for (int i = 1; i <= 4; i++) {
            Files.deleteIfExists(Paths.get("player" + i + "_output.txt"));
            Files.deleteIfExists(Paths.get("deck" + i + "_output.txt"));
        }
    }
}
