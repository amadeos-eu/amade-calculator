package eu.amadeos.apps.calculator.repository;

import eu.amadeos.apps.calculator.domain.AngleUnit;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.WordSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorPreferencesTest {
    @TempDir
    Path dir;

    @Test
    void defaultsWithoutAFile() {
        assertEquals(Settings.DEFAULT, new CalculatorPreferences(dir.resolve("calculator.properties")).settings());
    }

    @Test
    void settingsAreKept() {
        Path file = dir.resolve("calculator.properties");
        Settings settings = new Settings(Mode.PROGRAMMER, true, false, AngleUnit.RADIANS, Radix.BIN, WordSize.WORD, false);
        new CalculatorPreferences(file).setSettings(settings);
        assertEquals(settings, new CalculatorPreferences(file).settings());
    }

    @Test
    void brokenValuesFallBackToTheDefaults() throws Exception {
        Path file = dir.resolve("calculator.properties");
        Files.writeString(file, "mode=abacus\nword-size=12\nradix=roman\nangles=gon\n");
        assertEquals(Settings.DEFAULT, new CalculatorPreferences(file).settings());
    }
}
