package card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PackReader}.
 *
 * <p>These tests use JUnit 5's {@code @TempDir} to create pack files in a
 * temporary directory that is deleted automatically after each test. Each
 * test writes a small, focused pack file and verifies one behaviour: a valid
 * read, or a specific kind of validation failure. This isolation makes the
 * suite reliable on every run.</p>
 */
class PackReaderTest {

    /**
     * JUnit 5 supplies a fresh temporary directory for each test, then deletes
     * it automatically. Test pack files live here, not in the project folder.
     */
    @TempDir
    Path tempDir;

    /**
     * Writes the given lines to a file inside the temp dir and returns its
     * absolute path as a string, ready to pass to the reader under test.
     */
    private String writePack(String... lines) throws IOException {
        Path file = tempDir.resolve("pack.txt");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
        }
        Files.writeString(file, sb.toString());
        return file.toString();
    }

    @Test
    @DisplayName("A valid 8n-line pack is read into 8n cards, in order")
    void validPackReadsAllCardsInOrder() throws Exception {
        String[] lines = new String[32];
        for (int i = 0; i < 32; i++) lines[i] = String.valueOf(i);
        String path = writePack(lines);

        List<Card> cards = PackReader.readAndValidate(path, 4);

        assertEquals(32, cards.size());
        assertEquals(0, cards.get(0).getValue());
        assertEquals(31, cards.get(31).getValue());
    }

    @Test
    @DisplayName("A non-existent file throws IOException, not a validation error")
    void missingFileThrowsIOException() {
        assertThrows(IOException.class,
                () -> PackReader.readAndValidate(
                        tempDir.resolve("does-not-exist.txt").toString(), 4));
    }

    @Test
    @DisplayName("Too few rows is rejected as invalid")
    void tooFewRowsRejected() throws Exception {
        String path = writePack("1", "2", "3");
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 4));
    }

    @Test
    @DisplayName("Too many rows is rejected as invalid")
    void tooManyRowsRejected() throws Exception {
        String[] lines = new String[40];
        for (int i = 0; i < 40; i++) lines[i] = "1";
        String path = writePack(lines);
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 4));
    }

    @Test
    @DisplayName("A non-integer row is rejected as invalid")
    void nonIntegerRowRejected() throws Exception {
        String[] lines = new String[32];
        for (int i = 0; i < 32; i++) lines[i] = "1";
        lines[5] = "apple";
        String path = writePack(lines);
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 4));
    }

    @Test
    @DisplayName("A negative integer row is rejected as invalid")
    void negativeRowRejected() throws Exception {
        String[] lines = new String[32];
        for (int i = 0; i < 32; i++) lines[i] = "1";
        lines[10] = "-3";
        String path = writePack(lines);
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 4));
    }

    @Test
    @DisplayName("An empty row is rejected as invalid")
    void emptyRowRejected() throws Exception {
        String[] lines = new String[32];
        for (int i = 0; i < 32; i++) lines[i] = "1";
        lines[7] = "";
        String path = writePack(lines);
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 4));
    }

    @Test
    @DisplayName("A zero or negative player count is rejected")
    void nonPositiveNRejected() throws Exception {
        String path = writePack("1");
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, 0));
        assertThrows(InvalidPackException.class,
                () -> PackReader.readAndValidate(path, -1));
    }
}
