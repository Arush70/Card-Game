package card;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a pack file from disk and validates its contents.
 *
 * <p>A valid pack is a plain-text file with exactly {@code 8n} lines, where
 * each line contains a single non-negative integer. This class is responsible
 * for both reading the file (which may fail with an {@link IOException}) and
 * validating its contents (which fails with an {@link InvalidPackException}).
 * Keeping the two failure modes distinct lets the caller give the user a
 * precise error message and re-prompt for the right thing.</p>
 *
 * <p>This class has no mutable state, so it is trivially thread-safe.</p>
 */
public final class PackReader {

    /** Each player holds 4 cards and each deck holds 4 cards, giving 8n. */
    private static final int CARDS_PER_PLAYER_AND_DECK = 8;

    /** Utility class: not intended to be instantiated. */
    private PackReader() { }

    /**
     * Reads and validates a pack file for a game of {@code n} players.
     *
     * @param path the file path supplied by the user
     * @param n the number of players (must be a positive integer)
     * @return the cards in pack order, the first one being the top of the pack
     * @throws IOException if the file cannot be opened or read
     * @throws InvalidPackException if the file's contents are not a valid pack
     */
    public static List<Card> readAndValidate(String path, int n)
            throws IOException, InvalidPackException {

        if (n <= 0) {
            throw new InvalidPackException(
                    "Number of players must be positive, but was " + n);
        }

        int expectedLines = CARDS_PER_PLAYER_AND_DECK * n;
        List<Card> cards = new ArrayList<>(expectedLines);

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    throw new InvalidPackException(
                            "Line " + lineNumber + " is empty; "
                                    + "each row must contain a single non-negative integer");
                }
                int value;
                try {
                    value = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new InvalidPackException(
                            "Line " + lineNumber + " is not an integer: '" + trimmed + "'");
                }
                if (value < 0) {
                    throw new InvalidPackException(
                            "Line " + lineNumber + " is negative: " + value
                                    + "; card values must be non-negative");
                }
                cards.add(new Card(value));
            }
        }

        if (cards.size() != expectedLines) {
            throw new InvalidPackException(
                    "Pack has " + cards.size() + " cards, but " + expectedLines
                            + " were expected for " + n + " players");
        }

        return cards;
    }
}
