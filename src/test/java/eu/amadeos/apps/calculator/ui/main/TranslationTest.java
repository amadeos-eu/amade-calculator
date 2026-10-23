package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.bootstrap.CalculatorModule;
import eu.amadeos.apps.calculator.domain.CalculationException;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationTest {
    private static Messages texts(Language language) {
        return Messages.load(CalculatorModule.class, CalculatorModule.MESSAGES, Locale.forLanguageTag(language.code()));
    }

    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(CalculatorModule.class, CalculatorModule.MESSAGES));
    }

    @Test
    void everyErrorModeWordSizeAndBaseHasAText() {
        Messages msg = MainPresenterTest.MSG;
        for (String key : List.of(CalculationException.DIVISION_BY_ZERO, CalculationException.INVALID_INPUT,
                CalculationException.OVERFLOW)) {
            assertTrue(msg.has(key), key);
        }
        for (Mode mode : Mode.values()) assertTrue(msg.has("mode." + mode.key()));
        for (WordSize word : WordSize.values()) assertTrue(msg.has("word." + word.name().toLowerCase()));
        for (Radix radix : Radix.values()) assertTrue(msg.has("radix." + radix.name().toLowerCase() + ".tip"));
    }

    /** Every key but the digits and the four basic operators explains itself in a tooltip. */
    @Test
    void keysHaveTooltips() {
        List<Key> plain = List.of(Key.POINT, Key.ADD, Key.SUBTRACT, Key.MULTIPLY, Key.DIVIDE);
        for (Key key : Key.values()) {
            if (key.kind() == Key.Kind.DIGIT || plain.contains(key)) continue;
            assertTrue(MainPresenterTest.MSG.has("key." + key.id() + ".tip"), key.toString());
        }
    }

    /** No two entries share a mnemonic letter in the menu bar, in a menu or in the View menu. */
    @Test
    void mnemonicsAreUnique() {
        Map<String, List<String>> groups = new HashMap<>();
        groups.put("bar", List.of("menu.calculator", "menu.edit", "menu.view", "menu.options", "menu.help"));
        groups.put("calculator", List.of("menu.calculator.clear", "menu.calculator.exit"));
        groups.put("edit", List.of("menu.edit.copy", "menu.edit.paste"));
        List<String> view = new ArrayList<>();
        for (Mode mode : Mode.values()) view.add("mode." + mode.key());
        view.addAll(List.of("menu.view.tape", "menu.view.grouping"));
        groups.put("view", view);
        for (Language language : Language.values()) {
            Messages texts = texts(language);
            for (Map.Entry<String, List<String>> group : groups.entrySet()) {
                Map<Integer, String> used = new HashMap<>();
                for (String key : group.getValue()) {
                    Mnemonics.Text text = Mnemonics.parse(texts.get(key));
                    assertTrue(text.index() >= 0, language + " " + key + " has no mnemonic");
                    String before = used.put(text.keyCode(), key);
                    assertEquals(null, before, language + " " + group.getKey() + ": " + key + " and " + before
                            + " share a letter");
                }
            }
        }
    }

    @Test
    void windowTitles() {
        assertEquals("Rechner", texts(Language.GERMAN).get("window.title"));
        assertFalse(texts(Language.FRENCH).get("error.overflow").isBlank());
    }
}
