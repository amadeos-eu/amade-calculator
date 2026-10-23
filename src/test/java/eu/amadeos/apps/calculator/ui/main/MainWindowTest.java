package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.OnEdt;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.apps.calculator.repository.CalculatorPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window: keypads per mode, clicks, typing, the paper tape and the Programmer controls. Needs a display. */
class MainWindowTest {
    @TempDir
    Path dir;

    private MainWindow window;
    private MainPresenter presenter;

    private void open(Settings settings) throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        CalculatorPreferences preferences = new CalculatorPreferences(dir.resolve("calculator.properties"));
        preferences.setSettings(settings);
        OnEdt.run(() -> {
            window = new MainWindow(MainPresenterTest.MSG, null);
            presenter = new MainPresenter(window, MainPresenterTest.MSG, null, preferences, Locale.US, () -> 0.5, () -> { });
            window.bind(presenter);
            presenter.start();
        });
    }

    private void close() throws Exception {
        OnEdt.run(() -> window.close());
    }

    private void click(Key... keys) throws Exception {
        OnEdt.run(() -> {
            for (Key key : keys) window.key(key).orElseThrow(() -> new AssertionError(key + " not shown")).doClick(0);
        });
    }

    private void type(char c) throws Exception {
        OnEdt.run(() -> window.dispatchForTest(new KeyEvent(window, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c)));
    }

    private void press(int keyCode) throws Exception {
        OnEdt.run(() -> window.dispatchForTest(new KeyEvent(window, KeyEvent.KEY_PRESSED, 0, 0, keyCode,
                KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void basicKeypadComputes() throws Exception {
        open(Settings.DEFAULT);
        try {
            click(Key.D1, Key.D2, Key.ADD, Key.D3, Key.MULTIPLY, Key.D4, Key.EQUALS);
            OnEdt.run(() -> {
                assertEquals("24", window.display().valueText());
                assertEquals("12 + 3 × 4 =", window.display().expressionText());
                assertTrue(window.key(Key.SIN).isEmpty());
                assertEquals(".", window.key(Key.POINT).orElseThrow().getText());
                assertFalse(window.tape().isVisible());
            });
        } finally {
            close();
        }
    }

    @Test
    void typingWorksLikeTheKeys() throws Exception {
        open(Settings.DEFAULT);
        try {
            for (char c : "7*6".toCharArray()) type(c);
            press(KeyEvent.VK_ENTER);
            OnEdt.run(() -> assertEquals("42", window.display().valueText()));
            press(KeyEvent.VK_BACK_SPACE);
            type('5');
            press(KeyEvent.VK_BACK_SPACE);
            OnEdt.run(() -> assertEquals("0", window.display().valueText()));
        } finally {
            close();
        }
    }

    // --8<-- [start:enter-once]
    /** A typed key lights up its button; releasing the button must not press it a second time. */
    @Test
    void enterComputesOnce() throws Exception {
        open(Settings.DEFAULT);
        try {
            OnEdt.run(() -> window.setVisible(true));
            for (char c : "2+3".toCharArray()) type(c);
            press(KeyEvent.VK_ENTER);
            Thread.sleep(400);
            OnEdt.run(() -> {
                assertEquals("5", window.display().valueText());
                assertEquals(1, presenter.tape().size());
            });
        } finally {
            close();
        }
    }
    // --8<-- [end:enter-once]

    @Test
    void scientificAndThePaperTape() throws Exception {
        open(Settings.DEFAULT.withMode(Mode.SCIENTIFIC).withPaperTape(true));
        try {
            click(Key.D3, Key.D0, Key.SIN, Key.ADD, Key.D1, Key.D6, Key.SQUARE_ROOT, Key.EQUALS);
            OnEdt.run(() -> {
                assertEquals("4.5", window.display().valueText());
                assertTrue(window.tape().isVisible());
                assertEquals("sin(30) + √(16)\n= 4.5", window.tape().text().strip());
                assertEquals("DEG", window.key(Key.ANGLE).orElseThrow().getText());
            });
            click(Key.SECOND);
            OnEdt.run(() -> {
                assertEquals("sin⁻¹", window.key(Key.SIN).orElseThrow().getText());
                assertTrue(window.display().indicatorText().contains("2nd"));
            });
            click(Key.ANGLE);
            OnEdt.run(() -> assertEquals("RAD", window.key(Key.ANGLE).orElseThrow().getText()));
            OnEdt.run(() -> window.menu().tapeItem().doClick(0));
            OnEdt.run(() -> assertFalse(window.tape().isVisible()));
        } finally {
            close();
        }
    }

    @Test
    void programmerControls() throws Exception {
        open(Settings.DEFAULT.withMode(Mode.PROGRAMMER).withRadix(Radix.HEX).withWord(WordSize.BYTE).withSigned(false));
        try {
            OnEdt.run(() -> {
                JButton a = window.key(Key.HEX_A).orElseThrow();
                assertTrue(a.isEnabled());
                assertEquals("AC", window.key(Key.CLEAR).orElseThrow().getText());
                assertTrue(window.radixButton(Radix.HEX).isSelected());
                assertEquals(WordSize.BYTE, window.wordSizeBox().getSelectedItem());
            });
            type('f');
            type('0');
            OnEdt.run(() -> {
                assertEquals("F0", window.display().valueText());
                assertEquals("240", window.radixValue(Radix.DEC));
                assertEquals(0xF0, window.bitField().bits().intValue());
            });
            OnEdt.run(() -> window.radixButton(Radix.BIN).doClick(0));
            OnEdt.run(() -> {
                assertEquals("1111 0000", window.display().valueText());
                assertFalse(window.key(Key.D2).orElseThrow().isEnabled());
                assertFalse(window.key(Key.HEX_A).orElseThrow().isEnabled());
            });
            OnEdt.run(() -> window.wordSizeBox().setSelectedItem(WordSize.WORD));
            OnEdt.run(() -> assertEquals(WordSize.WORD, presenter.settings().word()));
            OnEdt.run(() -> window.menu().modeItem(Mode.BASIC).doClick(0));
            OnEdt.run(() -> {
                assertEquals("240", window.display().valueText());
                assertTrue(window.key(Key.HEX_A).isEmpty());
            });
        } finally {
            close();
        }
    }
}
