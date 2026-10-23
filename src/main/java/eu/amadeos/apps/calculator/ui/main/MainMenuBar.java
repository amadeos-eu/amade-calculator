package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Calculator, Edit, View, Options and Help. */
public final class MainMenuBar extends JMenuBar {
    private final Messages msg;
    private final Map<Mode, JRadioButtonMenuItem> modes = new EnumMap<>(Mode.class);
    private final JCheckBoxMenuItem tape;
    private final JCheckBoxMenuItem grouping;
    private boolean updating;

    public MainMenuBar(MainPresenter presenter, Messages msg, LocaleSettings locales) {
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JMenu calculator = menu("menu.calculator");
        calculator.add(item("menu.calculator.clear", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                () -> presenter.press(Key.CLEAR)));
        calculator.addSeparator();
        calculator.add(item("menu.calculator.exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK),
                presenter::exit));

        JMenu edit = menu("menu.edit");
        edit.add(item("menu.edit.copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
                presenter::copy));
        edit.add(item("menu.edit.paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK),
                presenter::paste));

        JMenu view = menu("menu.view");
        ButtonGroup group = new ButtonGroup();
        int number = KeyEvent.VK_1;
        for (Mode mode : Mode.values()) {
            JRadioButtonMenuItem item = Mnemonics.apply(new JRadioButtonMenuItem(), msg.get("mode." + mode.key()));
            item.setName("mode." + mode.key());
            item.setAccelerator(KeyStroke.getKeyStroke(number++, InputEvent.CTRL_DOWN_MASK));
            item.addActionListener(e -> {
                if (!updating) presenter.setMode(mode);
            });
            group.add(item);
            modes.put(mode, item);
            view.add(item);
        }
        view.addSeparator();
        tape = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.view.tape"));
        tape.setName("menu.view.tape");
        tape.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        tape.addActionListener(e -> {
            if (!updating) presenter.setPaperTape(tape.isSelected());
        });
        view.add(tape);
        grouping = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.view.grouping"));
        grouping.setName("menu.view.grouping");
        grouping.addActionListener(e -> {
            if (!updating) presenter.setGrouping(grouping.isSelected());
        });
        view.add(grouping);

        JMenu options = menu("menu.options");
        if (locales != null) options.add(languageMenu(presenter, locales));

        JMenu help = menu("menu.help");
        help.add(item("menu.help.about", null, presenter::about));
    }

    void update(Settings settings) {
        updating = true;
        try {
            modes.get(settings.mode()).setSelected(true);
            tape.setSelected(settings.paperTape());
            grouping.setSelected(settings.grouping());
        } finally {
            updating = false;
        }
    }

    JRadioButtonMenuItem modeItem(Mode mode) {
        return modes.get(mode);
    }

    JCheckBoxMenuItem tapeItem() {
        return tape;
    }

    /** "Desktop setting (Deutsch)" and the five languages; the choice applies at the next start. */
    private JMenu languageMenu(MainPresenter presenter, LocaleSettings locales) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get("menu.options.language"));
        menu.setName("menu.options.language");
        Messages commons = Messages.commons(msg.locale());
        List<Optional<Language>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        for (Language language : Language.values()) choices.add(Optional.of(language));
        ButtonGroup group = new ButtonGroup();
        Optional<Language> current = locales.applicationLanguage();
        for (Optional<Language> choice : choices) {
            String label = choice.map(Language::nativeName).orElseGet(() -> commons.format("language.follow-desktop",
                    locales.desktopResolution().language().nativeName()));
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, choice.equals(current));
            item.setName(choice.map(Language::code).orElse(LocaleSettings.FOLLOW_DESKTOP));
            item.addActionListener(e -> presenter.setLanguage(choice));
            group.add(item);
            menu.add(item);
            if (choice.isEmpty()) menu.addSeparator();
        }
        return menu;
    }

    private JMenu menu(String key) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get(key));
        menu.setName(key);
        add(menu);
        return menu;
    }

    private JMenuItem item(String key, KeyStroke accelerator, Runnable action) {
        JMenuItem item = Mnemonics.apply(new JMenuItem(), msg.get(key));
        item.setName(key);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(e -> action.run());
        return item;
    }
}
