package card;

import java.util.ArrayList;
import java.util.List;

/**
 * This will deal cards from the pack to players and decks.
 * Cards are distributed in order until each player
 * and deck starts with 4 cards.
 */
public final class Dealer {

    //Number of cards each player and deck starts playing the game
    public static final int CARDS_EACH = 4;

    private Dealer() { }

    /**
     * Deals cards to the players in sequence until each player
     * has four cards.
     *
     * @param pack the pack of cards
     * @param n number of players
     * @return a list containing each player's hand
     * @throws IllegalArgumentException if the pack does not
     *                                  contain enough cards
     */
    public static List<List<Card>> dealHands(List<Card> pack, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException(
                    "Number of players must be positive, but was " + n);
        }
        int needed = CARDS_EACH * n;
        if (pack.size() < needed) {
            throw new IllegalArgumentException(
                    "Pack has " + pack.size() + " cards but " + needed
                            + " are needed to deal " + n + " hands");
        }

        List<List<Card>> hands = new ArrayList<>(n);
        // Creating a hand for each player
        for (int i = 0; i < n; i++) {
            hands.add(new ArrayList<>(CARDS_EACH));
        }

        // Give cards to player one at a time
        for (int i = 0; i < needed; i++) {
            hands.get(i % n).add(pack.get(i));
        }
        return hands;
    }


    /**
     * Fills the decks using the remaining cards after
     * the player hands have been given required cards.
     *
     * @param pack the same pack used for dealing hands
     * @param n number of players and decks
     * @return a list of filled {@link CardDeck} objects
     * @throws IllegalArgumentException if there are not
     *                                  enough cards available
     */
    public static List<CardDeck> fillDecks(List<Card> pack, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException(
                    "Number of players must be positive, but was " + n);
        }
        // Overall cards needed
        int total = 2 * CARDS_EACH * n; 
        if (pack.size() < total) {
            throw new IllegalArgumentException(
                    "Pack has " + pack.size() + " cards but " + total
                            + " are needed for " + n + " hands and " + n + " decks");
        }

        List<CardDeck> decks = new ArrayList<>(n);
        
        // Creating the decks
        for (int i = 1; i <= n; i++) {
            decks.add(new CardDeck(i));
        }

        // Add the remaining cards to the decks
        int handsCardCount = CARDS_EACH * n;
        for (int i = 0; i < handsCardCount; i++) {
            decks.get(i % n).discard(pack.get(handsCardCount + i));
        }
        return decks;
    }
}
