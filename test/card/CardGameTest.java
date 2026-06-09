package card;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for CardGame.
 *
 * These tests force CardGame into terminal-input mode and pass simulated user 
 * input streams through System.setIn so the game routine runs non-interactively. 
 * The end-to-end test executes the full application pipeline: prompt, validate, 
 * deal, concurrent thread execution, and file output.
 */
class CardGameTest {

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void redirect() {
        originalIn = System.in;
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    // Restores the default system streams after each individual test finishes execution
    @AfterEach
    void restore() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // Feeds the given array of lines, joined by newlines, directly into System.in.
    private void feed(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        System.setIn(new ByteArrayInputStream(sb.toString().getBytes()));
    }

    // Writes a 32-row pack file rigged so player 1 wins immediately. 
    private static Path writeRiggedPack(Path dir) throws Exception {
        int[] vals = new int[32];
        for (int i = 0; i < 32; i++) vals[i] = (i % 4 == 0) ? 1 : 0;
        vals[1] = 7;  vals[5]  = 11; vals[9]  = 12; vals[13] = 13;
        vals[2] = 5;  vals[6]  = 14; vals[10] = 15; vals[14] = 16;
        vals[3] = 9;  vals[7]  = 17; vals[11] = 18; vals[15] = 19;
        StringBuilder sb = new StringBuilder();
        for (int v : vals) sb.append(v).append('\n');
        Path packFile = dir.resolve("rigged.txt");
        Files.writeString(packFile, sb.toString());
        return packFile;
    }

    @Test
    @DisplayName("Re-prompts for n when input is not a positive integer")
    void rePromptsForBadN() {
        feed("abc", "0", "-1", "4", "/tmp/does-not-exist.txt", "/tmp/still-not-here.txt");
        CardGame game = new CardGame(false);
        // Reading n until "4" is accepted, then it will start asking for the pack
        // and we exit by interrupting after enough fake inputs. We only call the
        // input-reading method directly to keep the test focused.
        assertEquals(4, game.promptForNumberOfPlayers());
    }

    @Test
    @DisplayName("Re-prompts for the pack when path is invalid, then accepts a valid pack")
    void rePromptsForBadPack(@TempDir Path tempDir) throws Exception {
    	// Uses the @TempDir annotation to handle sandbox file creations safely
        Path bad = tempDir.resolve("too-short.txt");
        Files.writeString(bad, "1\n2\n3\n");
        Path good = writeRiggedPack(tempDir);

        feed(
                "/no/such/file.txt",     // Triggers IOException -> forces a re-prompt
                bad.toString(),          // Triggers row count validation mismatch -> forces a re-prompt
                good.toString()          // Valid dataset structure -> pack accepted successfully
        );
        CardGame game = new CardGame(false);
        List<Card> cards = game.promptForValidPack(4);
        assertEquals(32, cards.size());
    }

    @Test
    @DisplayName("End-to-end: a full game runs and writes 2n output files")
    void endToEndFullGame(@TempDir Path tempDir) throws Exception {
    	// Uses the @TempDir parameter to safely isolate file system operations
        Path pack = writeRiggedPack(tempDir);

        // Run the game simulation inside the isolated directory so I/O side effects
        // don't land in the project root. We update the user.dir system property right 
        // before calling play() since standard Java IO relative paths honour this on most JREs.
        String originalDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", tempDir.toString());
            feed("4", pack.toString());
            new CardGame(false).play();
        } finally {
            System.setProperty("user.dir", originalDir);
        }

        // The output files may end up in either user.dir or the JVM working
        // directory, so checking both.
        for (int i = 1; i <= 4; i++) {
            assertTrue(
                    Files.exists(tempDir.resolve("player" + i + "_output.txt"))
                            || Files.exists(Paths.get("player" + i + "_output.txt")),
                    "player" + i + "_output.txt was not written"
            );
            assertTrue(
                    Files.exists(tempDir.resolve("deck" + i + "_output.txt"))
                            || Files.exists(Paths.get("deck" + i + "_output.txt")),
                    "deck" + i + "_output.txt was not written"
            );
        }

        // The terminal should contain the win announcement.
        String output = capturedOut.toString();
        assertTrue(output.contains("player 1 wins"),
                "Expected 'player 1 wins' on stdout, got: " + output);

        // Tidy up files that may have landed in the project working dir.
        for (int i = 1; i <= 4; i++) {
            Files.deleteIfExists(Paths.get("player" + i + "_output.txt"));
            Files.deleteIfExists(Paths.get("deck" + i + "_output.txt"));
        }
    }
}
