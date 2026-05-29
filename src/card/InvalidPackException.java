package card;

/**
 * thrown when the contents of a pack file don't match the desired number
 * of Players. A pack is valid only if it contains exactly {@code 8n} lines and
 * every line is a single non-negative integer.
 *
 * <p>This exception is distinct from {@link java.io.IOException}, so callers
 * can tell the difference between "the file could not be read" and "the file
 * was read but its contents are wrong," and prompt the user accordingly.</p>
 */
public class InvalidPackException extends Exception {

    /** Standard serial version id for a checked exception. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new validation exception with a human-readable message.
     *
     * @param message the reason the pack is invalid
     */
    public InvalidPackException(String message) {
        super(message);
    }
}
