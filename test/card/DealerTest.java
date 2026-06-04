package card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link Dealer}.
 *
 * <p>These tests build a pack of integers 1..8n where each card's value equals
 * its position in the pack. That makes it easy to verify, by exact value, that
 * the round-robin distribution sends the right cards to the right places.</p>
 */
class DealerTest {

	// Creating a pack of 8n cards with values from 1 to 8n
    private static List<Card> sequentialPack(int n) {
        List<Card> pack = new ArrayList<>(8 * n);
        for (int i = 1; i <= 8 * n; i++) {
            pack.add(new Card(i));
        }
        return pack;
    }

    @Test
    @DisplayName("Dealing four players produces four hands of four cards each")
    void fourHandsOfFourCards() {
        List<List<Card>> hands = Dealer.dealHands(sequentialPack(4), 4);
        assertEquals(4, hands.size());
        for (List<Card> hand : hands) {
            assertEquals(4, hand.size());
        }
    }

    @Test
    @DisplayName("Round-robin: player 1 receives cards 1, 5, 9, 13 (for n=4)")
    void player1GetsRoundRobinCards() {
        List<List<Card>> hands = Dealer.dealHands(sequentialPack(4), 4);
        List<Card> p1 = hands.get(0);
        assertEquals(1,  p1.get(0).getValue());
        assertEquals(5,  p1.get(1).getValue());
        assertEquals(9,  p1.get(2).getValue());
        assertEquals(13, p1.get(3).getValue());
    }

    @Test
    @DisplayName("Round-robin: player 4 receives cards 4, 8, 12, 16 (for n=4)")
    void player4GetsRoundRobinCards() {
        List<List<Card>> hands = Dealer.dealHands(sequentialPack(4), 4);
        List<Card> p4 = hands.get(3);
        assertEquals(4,  p4.get(0).getValue());
        assertEquals(8,  p4.get(1).getValue());
        assertEquals(12, p4.get(2).getValue());
        assertEquals(16, p4.get(3).getValue());
    }

    @Test
    @DisplayName("Filling decks produces n decks of four cards each")
    void fourDecksOfFourCards() {
        List<CardDeck> decks = Dealer.fillDecks(sequentialPack(4), 4);
        assertEquals(4, decks.size());
        for (CardDeck deck : decks) {
            assertEquals(4, deck.size());
        }
    }

    @Test
    @DisplayName("Deck 1 receives cards 17, 21, 25, 29 (for n=4) on top to bottom")
    void deck1GetsRoundRobinCards() {
        List<CardDeck> decks = Dealer.fillDecks(sequentialPack(4), 4);
        CardDeck d1 = decks.get(0);
        // Cards should come out in the same order they were added
        assertEquals(17, d1.draw().getValue());
        assertEquals(21, d1.draw().getValue());
        assertEquals(25, d1.draw().getValue());
        assertEquals(29, d1.draw().getValue());
    }

    @Test
    @DisplayName("Deck 4 receives cards 20, 24, 28, 32 (for n=4)")
    void deck4GetsRoundRobinCards() {
        List<CardDeck> decks = Dealer.fillDecks(sequentialPack(4), 4);
        CardDeck d4 = decks.get(3);
        assertEquals(20, d4.draw().getValue());
        assertEquals(24, d4.draw().getValue());
        assertEquals(28, d4.draw().getValue());
        assertEquals(32, d4.draw().getValue());
    }

    @Test
    @DisplayName("Conservation: every card in the pack ends up in exactly one place")
    void everyCardAccountedFor() {
        int n = 3;
        List<Card> pack = sequentialPack(n);
        List<List<Card>> hands = Dealer.dealHands(pack, n);
        List<CardDeck> decks = Dealer.fillDecks(pack, n);

        int handCount = 0;
        for (List<Card> hand : hands) handCount += hand.size();
        int deckCount = 0;
        for (CardDeck deck : decks) deckCount += deck.size();

        assertEquals(8 * n, handCount + deckCount);
    }

    @Test
    @DisplayName("Works for n=1 (smallest valid game)")
    void worksForSingleplayer() {
        List<List<Card>> hands = Dealer.dealHands(sequentialPack(1), 1);
        List<CardDeck> decks = Dealer.fillDecks(sequentialPack(1), 1);
        assertEquals(4, hands.get(0).size());
        assertEquals(4, decks.get(0).size());
    }

    @Test
    @DisplayName("Rejects non-positive n")
    void rejectsNonPositiveN() {
        List<Card> pack = sequentialPack(4);
        assertThrows(IllegalArgumentException.class, () -> Dealer.dealHands(pack, 0));
        assertThrows(IllegalArgumentException.class, () -> Dealer.fillDecks(pack, -1));
    }

    @Test
    @DisplayName("Rejects an undersized pack")
    void rejectsSmallPack() {
        List<Card> small = new ArrayList<>();
        small.add(new Card(1));
        assertThrows(IllegalArgumentException.class, () -> Dealer.dealHands(small, 4));
        assertThrows(IllegalArgumentException.class, () -> Dealer.fillDecks(small, 4));
    }
}
