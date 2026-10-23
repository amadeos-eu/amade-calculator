package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.TapeEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A window that remembers what it was asked to show. */
class FakeView implements MainView {
    Settings settings;
    DisplayState display;
    final List<TapeEntry> tape = new ArrayList<>();
    String clipboard;
    int beeps;
    final List<String> messages = new ArrayList<>();
    boolean closed;

    @Override
    public void showSettings(Settings newSettings) {
        settings = newSettings;
    }

    @Override
    public void showDisplay(DisplayState state) {
        display = state;
    }

    @Override
    public void addTape(TapeEntry entry) {
        tape.add(entry);
    }

    @Override
    public void clearTape() {
        tape.clear();
    }

    @Override
    public void copyToClipboard(String text) {
        clipboard = text;
    }

    @Override
    public Optional<String> clipboardText() {
        return Optional.ofNullable(clipboard);
    }

    @Override
    public void beep() {
        beeps++;
    }

    @Override
    public void showError(String message) {
        messages.add(message);
    }

    @Override
    public void showInfo(String message) {
        messages.add(message);
    }

    @Override
    public void showAbout() {
        messages.add("about");
    }

    @Override
    public void close() {
        closed = true;
    }
}
