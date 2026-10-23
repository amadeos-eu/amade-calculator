package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.TapeEntry;

import java.util.Optional;

/** The window as the presenter sees it. All methods run on the EDT. */
public interface MainView {
    /** Shows the mode with its keypads, and the menu and option states. */
    void showSettings(Settings settings);

    void showDisplay(DisplayState state);

    void addTape(TapeEntry entry);

    void clearTape();

    void copyToClipboard(String text);

    /** The text on the clipboard, if it holds text. */
    Optional<String> clipboardText();

    /** A key that does nothing right now, e.g. pasting something that is no number. */
    void beep();

    void showError(String message);

    void showInfo(String message);

    void showAbout();

    void close();
}
