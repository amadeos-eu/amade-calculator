package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.OnEdt;
import eu.amadeos.apps.calculator.bootstrap.CalculatorModule;
import eu.amadeos.apps.calculator.domain.AngleUnit;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.TapeEntry;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.apps.calculator.repository.CalculatorPreferences;
import eu.amadeos.commons.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainPresenterTest {
    static final Messages MSG = Messages.load(CalculatorModule.class, CalculatorModule.MESSAGES, Locale.US);

    @TempDir
    Path dir;

    private FakeView view;
    private MainPresenter presenter;
    private boolean exited;

    private void start(Settings settings) throws Exception {
        CalculatorPreferences preferences = new CalculatorPreferences(dir.resolve("calculator.properties"));
        preferences.setSettings(settings);
        view = new FakeView();
        OnEdt.run(() -> {
            presenter = new MainPresenter(view, MSG, null, preferences, Locale.US, () -> 0.25, () -> exited = true);
            presenter.start();
        });
    }

    private void type(String text) throws Exception {
        OnEdt.run(() -> {
            for (char c : text.toCharArray()) {
                if (c == ' ') continue;
                assertTrue(presenter.typed(c), "typed " + c);
            }
        });
    }

    private void press(Key... keys) throws Exception {
        OnEdt.run(() -> {
            for (Key key : keys) presenter.press(key);
        });
    }

    @Test
    void startsInTheSavedMode() throws Exception {
        start(Settings.DEFAULT.withMode(Mode.SCIENTIFIC));
        assertEquals(Mode.SCIENTIFIC, view.settings.mode());
        assertEquals("0", view.display.value());
        assertTrue(view.display.radixValues().isEmpty());
        assertNull(view.display.bits());
    }

    @Test
    void keyboardAndTape() throws Exception {
        start(Settings.DEFAULT);
        type("1234.5 * 3 =");
        assertEquals("3,703.5", view.display.value());
        assertEquals("1,234.5 × 3 =", view.display.expression());
        assertEquals(List.of(new TapeEntry("1,234.5 × 3", "3,703.5")), view.tape);
        assertEquals("1,234.5 × 3\n= 3,703.5\n\n", presenter.tapeText());
        OnEdt.run(presenter::copyTape);
        assertEquals("1,234.5 × 3\n= 3,703.5\n\n", view.clipboard);
        OnEdt.run(presenter::clearTape);
        assertTrue(view.tape.isEmpty());
        assertTrue(presenter.tape().isEmpty());
    }

    @Test
    void keysOfOtherModesDoNothing() throws Exception {
        start(Settings.DEFAULT);
        assertFalse(presenter.enabled(Key.SIN));
        assertFalse(presenter.enabled(Key.HEX_A));
        OnEdt.run(() -> assertFalse(presenter.typed('a')));
        OnEdt.run(() -> assertFalse(presenter.typed('&')));
        assertTrue(presenter.enabled(Key.D7));
    }

    @Test
    void secondSwitchesToTheInverseOnce() throws Exception {
        start(Settings.DEFAULT.withMode(Mode.SCIENTIFIC));
        type("0.5");
        press(Key.SECOND);
        assertTrue(view.display.second());
        press(Key.SIN);
        assertFalse(view.display.second());
        assertEquals("30", view.display.value());
        assertEquals("sin⁻¹(0.5)", view.display.expression());
    }

    @Test
    void angleKeySwitchesAndIsSaved() throws Exception {
        start(Settings.DEFAULT.withMode(Mode.SCIENTIFIC));
        press(Key.ANGLE);
        assertEquals(AngleUnit.RADIANS, view.settings.angles());
        assertEquals(AngleUnit.RADIANS, new CalculatorPreferences(dir.resolve("calculator.properties")).settings().angles());
        press(Key.PI, Key.COS);
        assertEquals("−1", view.display.value());
    }

    @Test
    void errorsShowTheirText() throws Exception {
        start(Settings.DEFAULT);
        type("1 / 0 =");
        assertTrue(view.display.error());
        assertEquals("Cannot divide by zero", view.display.value());
        OnEdt.run(presenter::copy);
        assertEquals(1, view.beeps);
        type("4");
        assertFalse(view.display.error());
    }

    @Test
    void programmerShowsEveryBaseAndTheBits() throws Exception {
        start(Settings.DEFAULT.withMode(Mode.PROGRAMMER).withRadix(Radix.HEX).withWord(WordSize.WORD));
        type("ff00 | 2a =");
        assertEquals("FF2A", view.display.value());
        assertEquals("−214", view.display.radixValues().get(Radix.DEC));
        assertEquals("1111 1111 0010 1010", view.display.radixValues().get(Radix.BIN));
        assertEquals(0xFF2A, view.display.bits().intValue());
        OnEdt.run(() -> presenter.flipBit(15));
        assertEquals("7F2A", view.display.value());
        OnEdt.run(() -> presenter.setRadix(Radix.DEC));
        assertEquals("32,554", view.display.value());
        OnEdt.run(() -> presenter.setSigned(false));
        OnEdt.run(() -> presenter.setWordSize(WordSize.BYTE));
        assertEquals("42", view.display.value());
    }

    @Test
    void switchingModesKeepsTheNumber() throws Exception {
        start(Settings.DEFAULT);
        type("255.9");
        OnEdt.run(() -> presenter.setMode(Mode.PROGRAMMER));
        assertEquals("255", view.display.value());
        assertEquals("FF", view.display.radixValues().get(Radix.HEX));
        OnEdt.run(() -> presenter.setMode(Mode.BASIC));
        assertEquals("255", view.display.value());
    }

    @Test
    void copyAndPaste() throws Exception {
        start(Settings.DEFAULT);
        type("1234.5");
        OnEdt.run(presenter::copy);
        assertEquals("1234.5", view.clipboard);
        view.clipboard = "1.234,75";
        OnEdt.run(presenter::paste);
        assertEquals("1,234.75", view.display.value());
        view.clipboard = "hello";
        OnEdt.run(presenter::paste);
        assertEquals(1, view.beeps);
    }

    @Test
    void groupingCanBeSwitchedOff() throws Exception {
        start(Settings.DEFAULT);
        type("1234567");
        OnEdt.run(() -> presenter.setGrouping(false));
        assertEquals("1234567", view.display.value());
        assertFalse(view.settings.grouping());
    }

    @Test
    void randomAndMemoryIndicator() throws Exception {
        start(Settings.DEFAULT.withMode(Mode.SCIENTIFIC));
        press(Key.RANDOM);
        assertEquals("0.25", view.display.value());
        press(Key.MEMORY_ADD);
        assertTrue(view.display.memory());
        press(Key.MEMORY_CLEAR);
        assertFalse(view.display.memory());
    }

    @Test
    void exitClosesTheWindow() throws Exception {
        start(Settings.DEFAULT);
        OnEdt.run(presenter::exit);
        assertTrue(view.closed);
        assertTrue(exited);
    }
}
