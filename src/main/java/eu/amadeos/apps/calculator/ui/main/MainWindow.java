package eu.amadeos.apps.calculator.ui.main;

import com.google.inject.Inject;
import eu.amadeos.apps.calculator.bootstrap.AmadeCalculatorApplication;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.TapeEntry;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The window: the display, the keypads of the mode (in Programmer also the number bases, word size and bit field)
 * and, if switched on, the paper tape on the right. The keyboard works like the keypad.
 */
public final class MainWindow extends JFrame implements MainView {
    private final Messages msg;
    private final LocaleSettings locales;
    private final Display display = new Display();
    private final JPanel content = new JPanel(new BorderLayout());
    private final PaperTape tape;
    private final Map<Key, List<JButton>> keys = new EnumMap<>(Key.class);
    private final Map<Radix, JRadioButton> radixButtons = new EnumMap<>(Radix.class);
    private final Map<Radix, JLabel> radixValues = new EnumMap<>(Radix.class);
    private final JComboBox<WordSize> wordSize = new JComboBox<>(WordSize.values());
    private final JCheckBox signed;
    private final BitField bitField = new BitField();
    private final KeyEventDispatcher keyboard = this::dispatchKey;
    private MainPresenter presenter;
    private MainMenuBar menuBar;
    private Settings shown;
    private Mode building;
    private boolean updating;

    @Inject
    public MainWindow(Messages msg, LocaleSettings locales) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.locales = locales;
        this.tape = new PaperTape(msg);
        this.signed = new JCheckBox(msg.get("programmer.signed"));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-calculator.png");
        if (icon != null) setIconImage(new javax.swing.ImageIcon(icon).getImage());
        add(content, BorderLayout.CENTER);
        add(tape, BorderLayout.EAST);
        tape.setVisible(false);
    }

    public void bind(MainPresenter newPresenter) {
        EdtExecutor.requireEdt();
        presenter = newPresenter;
        menuBar = new MainMenuBar(presenter, msg, locales);
        setJMenuBar(menuBar);
        tape.onCopy(presenter::copyTape);
        tape.onClear(presenter::clearTape);
        bitField.onFlip(presenter::flipBit);
        wordSize.setName("programmer.word");
        wordSize.setFocusable(false);
        wordSize.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                String text = value instanceof WordSize w ? msg.get("word." + w.name().toLowerCase()) : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        wordSize.addActionListener(e -> {
            if (!updating && wordSize.getSelectedItem() instanceof WordSize w) presenter.setWordSize(w);
        });
        signed.setName("programmer.signed");
        signed.setFocusable(false);
        signed.addActionListener(e -> {
            if (!updating) presenter.setSigned(signed.isSelected());
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                presenter.exit();
            }
        });
        // The keys are not focusable, so the display holds the focus; menu shortcuts need a focused component.
        display.setFocusable(true);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (getFocusOwner() == null || getFocusOwner() == MainWindow.this) display.requestFocusInWindow();
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboard);
    }

    // ---------------------------------------------------------------- keyboard

    // --8<-- [start:keyboard]
    /** Typing goes to the calculator while this window is active and no text field or list has the focus. */
    private boolean dispatchKey(KeyEvent e) {
        Window window = SwingUtilities.getWindowAncestor(e.getComponent());
        if (e.getComponent() != this && window != this || presenter == null) return false;
        Component focus = e.getComponent();
        if (focus instanceof JTextComponent text && text.isEditable() || focus instanceof JComboBox<?>
                || focus instanceof JList<?>) {
            return false;
        }
        if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) return false;
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            Key key = switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER -> Key.EQUALS;
                case KeyEvent.VK_BACK_SPACE -> Key.BACKSPACE;
                case KeyEvent.VK_DELETE -> Key.CLEAR_ENTRY;
                default -> null;
            };
            if (key == null) return false;
            flash(key);
            presenter.press(key);
            return true;
        }
        if (e.getID() == KeyEvent.KEY_TYPED) {
            char c = e.getKeyChar();
            if (c == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(c)) return c == '\n' || c == '\b';
            Key key = presenter.keyFor(c);
            if (key == null || !presenter.enabled(key)) return false;
            flash(key);
            presenter.press(key);
            return true;
        }
        return false;
    }
    // --8<-- [end:keyboard]

    /** Shows a key as pressed for a moment when it was typed. */
    private void flash(Key key) {
        for (JButton button : keys.getOrDefault(key, List.of())) {
            if (button.isShowing()) {
                button.getModel().setArmed(true);
                button.getModel().setPressed(true);
                // Disarmed first: releasing an armed button would press it once more.
                javax.swing.Timer release = new javax.swing.Timer(90, e -> {
                    button.getModel().setArmed(false);
                    button.getModel().setPressed(false);
                });
                release.setRepeats(false);
                release.start();
            }
        }
    }

    // ---------------------------------------------------------------- layout

    @Override
    public void showSettings(Settings settings) {
        EdtExecutor.requireEdt();
        updating = true;
        try {
            menuBar.update(settings);
            boolean rebuild = shown == null || shown.mode() != settings.mode();
            if (rebuild) build(settings.mode());
            boolean tapeChanged = shown == null || shown.paperTape() != settings.paperTape();
            tape.setVisible(settings.paperTape());
            if (radixButtons.containsKey(settings.radix())) radixButtons.get(settings.radix()).setSelected(true);
            wordSize.setSelectedItem(settings.word());
            signed.setSelected(settings.signed());
            for (Map.Entry<Key, List<JButton>> entry : keys.entrySet()) {
                boolean enabled = presenter.enabled(entry.getKey());
                for (JButton button : entry.getValue()) button.setEnabled(enabled);
            }
            for (JButton button : keys.getOrDefault(Key.ANGLE, List.of())) button.setText(settings.angles().label());
            shown = settings;
            if (rebuild || tapeChanged) {
                pack();
                setMinimumSize(getPreferredSize());
                if (rebuild && !isVisible()) setLocationRelativeTo(null);
            }
        } finally {
            updating = false;
        }
    }

    private void build(Mode mode) {
        building = mode;
        content.removeAll();
        keys.clear();
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        switch (mode) {
            case BASIC -> {
                main.add(display, BorderLayout.NORTH);
                main.add(keypad(Keypads.BASIC), BorderLayout.CENTER);
            }
            case SCIENTIFIC -> {
                main.add(display, BorderLayout.NORTH);
                JPanel pads = new JPanel(new BorderLayout(10, 0));
                pads.add(keypad(Keypads.SCIENTIFIC), BorderLayout.CENTER);
                pads.add(keypad(Keypads.BASIC), BorderLayout.EAST);
                main.add(pads, BorderLayout.CENTER);
            }
            case PROGRAMMER -> {
                JPanel top = new JPanel(new BorderLayout(10, 0));
                top.add(display, BorderLayout.CENTER);
                top.add(radixPanel(), BorderLayout.EAST);
                main.add(top, BorderLayout.NORTH);
                JPanel left = new JPanel(new BorderLayout(0, 8));
                JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                JLabel wordLabel = new JLabel(msg.get("programmer.word"));
                wordLabel.setLabelFor(wordSize);
                options.add(wordLabel);
                options.add(wordSize);
                options.add(signed);
                left.add(options, BorderLayout.NORTH);
                bitField.setToolTipText(msg.get("bits.tip"));
                left.add(bitField, BorderLayout.CENTER);
                left.add(keypad(Keypads.PROGRAMMER_LOGIC), BorderLayout.SOUTH);
                JPanel pads = new JPanel(new BorderLayout(10, 0));
                pads.add(left, BorderLayout.CENTER);
                pads.add(keypad(Keypads.PROGRAMMER_DIGITS), BorderLayout.EAST);
                main.add(pads, BorderLayout.CENTER);
            }
        }
        content.add(main, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
        display.requestFocusInWindow();
    }

    private JPanel radixPanel() {
        JPanel panel = new JPanel(new GridLayout(Radix.values().length, 1, 0, 0));
        panel.setName("radix");
        radixButtons.clear();
        radixValues.clear();
        ButtonGroup group = new ButtonGroup();
        for (Radix radix : Radix.values()) {
            JRadioButton button = new JRadioButton(radix.name());
            button.setName("radix." + radix.name().toLowerCase());
            button.setFocusable(false);
            button.setFont(button.getFont().deriveFont(Font.BOLD));
            button.setToolTipText(msg.get("radix." + radix.name().toLowerCase() + ".tip"));
            button.addActionListener(e -> {
                if (!updating) presenter.setRadix(radix);
            });
            group.add(button);
            radixButtons.put(radix, button);
            JLabel value = new JLabel("0");
            value.setName("radix.value." + radix.name().toLowerCase());
            value.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(AmadeUI.scaled(13))));
            radixValues.put(radix, value);
            JPanel row = new JPanel(new BorderLayout(6, 0));
            button.setPreferredSize(new Dimension(Math.round(64 * AmadeUI.fontScale()), button.getPreferredSize().height));
            row.add(button, BorderLayout.WEST);
            row.add(value, BorderLayout.CENTER);
            panel.add(row);
        }
        // Wide enough for 64 bits in BIN: 16 groups of four.
        JLabel sample = radixValues.get(Radix.BIN);
        int width = sample.getFontMetrics(sample.getFont()).stringWidth("0000 0000 0000 0000 0000 0000 0000 0000");
        panel.setPreferredSize(new Dimension(width + Math.round(70 * AmadeUI.fontScale()),
                panel.getPreferredSize().height));
        return panel;
    }

    private JPanel keypad(Keypads.Block block) {
        JPanel panel = new JPanel(new GridLayout(0, block.columns(), 4, 4));
        for (Key key : block.keys()) panel.add(button(key));
        return panel;
    }

    private JButton button(Key key) {
        String label = key == Key.POINT ? String.valueOf(decimalSeparator()) : key.label();
        // Next to the hex digit C, clearing all is AC.
        if (key == Key.CLEAR && building == Mode.PROGRAMMER) label = "AC";
        JButton button = new JButton(label);
        button.setName("key." + key.id());
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        boolean large = key.kind() == Key.Kind.DIGIT || key.kind() == Key.Kind.OPERATOR || key.kind() == Key.Kind.EQUALS;
        button.setFont(new Font(Font.SANS_SERIF, large ? Font.BOLD : Font.PLAIN,
                Math.round(AmadeUI.scaled(large ? 16 : 13))));
        Dimension size = new Dimension(Math.round(52 * AmadeUI.fontScale()), Math.round(34 * AmadeUI.fontScale()));
        button.setPreferredSize(size);
        String tip = "key." + key.id() + ".tip";
        if (msg.has(tip)) button.setToolTipText(msg.get(tip));
        button.getAccessibleContext().setAccessibleName(msg.has(tip) ? msg.get(tip) : label);
        AmadeUI.style(button, (b, theme) -> {
            switch (key.kind()) {
                case DIGIT -> b.setBackground(theme.highlight());
                case OPERATOR -> b.setBackground(theme.softHighlight());
                case EQUALS -> {
                    b.setBackground(theme.selectionBackground());
                    b.setForeground(theme.selectionText());
                }
                default -> b.setBackground(theme.base());
            }
        });
        button.addActionListener(e -> presenter.press(key));
        keys.computeIfAbsent(key, k -> new ArrayList<>()).add(button);
        return button;
    }

    private char decimalSeparator() {
        return DecimalFormatSymbols.getInstance(msg.locale()).getDecimalSeparator();
    }

    // ---------------------------------------------------------------- MainView

    @Override
    public void showDisplay(DisplayState state) {
        EdtExecutor.requireEdt();
        List<String> indicators = new ArrayList<>();
        if (shown != null && shown.mode() == Mode.SCIENTIFIC) indicators.add(shown.angles().label());
        if (shown != null && shown.mode() == Mode.PROGRAMMER) {
            indicators.add(shown.radix().name());
            indicators.add(shown.word().name());
        }
        if (state.second()) indicators.add("2nd");
        if (state.memory()) indicators.add("M");
        if (state.openBrackets() > 0) indicators.add("(" + state.openBrackets());
        display.show(String.join("  ", indicators), state.expression(), state.value(), state.error());
        for (Map.Entry<Radix, JLabel> entry : radixValues.entrySet()) {
            entry.getValue().setText(state.radixValues().getOrDefault(entry.getKey(), ""));
        }
        if (state.bits() != null && shown != null) bitField.show(state.bits(), shown.word().bits());
        for (JButton button : keys.getOrDefault(Key.SECOND, List.of())) {
            button.getModel().setSelected(state.second());
            button.setBorderPainted(true);
        }
        for (Key key : List.of(Key.SIN, Key.COS, Key.TAN, Key.SINH, Key.COSH, Key.TANH)) {
            for (JButton button : keys.getOrDefault(key, List.of())) {
                button.setText(state.second() ? key.secondLabel() : key.label());
            }
        }
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
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    @Override
    public Optional<String> clipboardText() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return data instanceof String s ? Optional.of(s) : Optional.empty();
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    @Override
    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, plain(message), msg.get("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, plain(message), getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showAbout() {
        JOptionPane.showMessageDialog(this,
                "<html><center><h2>amade-calculator " + AmadeCalculatorApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; EUPL-1.2</small></center></html>",
                msg.get("dialog.about.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void close() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboard);
        dispose();
    }

    private static JTextArea plain(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        return text;
    }

    // ---------------------------------------------------------------- for tests

    MainMenuBar menu() {
        return menuBar;
    }

    Display display() {
        return display;
    }

    PaperTape tape() {
        return tape;
    }

    BitField bitField() {
        return bitField;
    }

    JComboBox<WordSize> wordSizeBox() {
        return wordSize;
    }

    JRadioButton radixButton(Radix radix) {
        return radixButtons.get(radix);
    }

    String radixValue(Radix radix) {
        return radixValues.get(radix).getText();
    }

    /** The first button of the key on the current keypad, if it has one. */
    Optional<JButton> key(Key key) {
        return keys.getOrDefault(key, List.of()).stream().findFirst();
    }

    boolean dispatchForTest(KeyEvent e) {
        return dispatchKey(e);
    }

    JComponent contentPanel() {
        return content;
    }
}
