package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.AngleUnit;
import eu.amadeos.apps.calculator.domain.Calculator;
import eu.amadeos.apps.calculator.domain.DecimalSystem;
import eu.amadeos.apps.calculator.domain.MathFunction;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.NumberSystem;
import eu.amadeos.apps.calculator.domain.Operator;
import eu.amadeos.apps.calculator.domain.PastedNumber;
import eu.amadeos.apps.calculator.domain.ProgrammerSystem;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.TapeEntry;
import eu.amadeos.apps.calculator.domain.Term;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.apps.calculator.repository.CalculatorPreferences;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.EdtExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/** Turns keys, typed characters and menu choices into calculator steps and shows the result. */
public final class MainPresenter {
    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);

    private final MainView view;
    private final Messages msg;
    private final LocaleSettings locales;
    private final CalculatorPreferences preferences;
    private final Locale locale;
    private final DoubleSupplier random;
    private final Runnable exit;
    private final List<TapeEntry> tape = new ArrayList<>();
    private Settings settings = Settings.DEFAULT;
    private Calculator calculator;
    private boolean second;

    /**
     * @param locales null in tests: no language menu
     * @param random  numbers for the Rnd key, from 0 (inclusive) to 1
     * @param exit    ends the program after the window closed
     */
    public MainPresenter(MainView view, Messages msg, LocaleSettings locales, CalculatorPreferences preferences,
                         Locale locale, DoubleSupplier random, Runnable exit) {
        this.view = view;
        this.msg = msg;
        this.locales = locales;
        this.preferences = preferences;
        this.locale = locale;
        this.random = random;
        this.exit = exit;
    }

    public void start() {
        EdtExecutor.requireEdt();
        settings = preferences.settings();
        calculator = new Calculator(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public Settings settings() {
        return settings;
    }

    Calculator calculator() {
        return calculator;
    }

    public List<TapeEntry> tape() {
        return List.copyOf(tape);
    }

    // ---------------------------------------------------------------- keys

    // --8<-- [start:press]
    public void press(Key key) {
        EdtExecutor.requireEdt();
        switch (key) {
            case D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, HEX_A, HEX_B, HEX_C, HEX_D, HEX_E, HEX_F ->
                    calculator.digit(key.digit());
            case POINT -> calculator.point();
            case ADD -> calculator.operator(Operator.ADD);
            case SUBTRACT -> calculator.operator(Operator.SUBTRACT);
            case MULTIPLY -> calculator.operator(Operator.MULTIPLY);
            case DIVIDE -> calculator.operator(Operator.DIVIDE);
            case PERCENT -> calculator.function(MathFunction.PERCENT);
            case EQUALS -> calculator.equals().ifPresent(this::addTape);
            case CLEAR -> calculator.clear();
            case CLEAR_ENTRY -> calculator.clearEntry();
            case BACKSPACE -> calculator.backspace();
            case NEGATE -> calculator.negate();
            case MEMORY_CLEAR -> calculator.memoryClear();
            case MEMORY_RECALL -> calculator.memoryRecall();
            case MEMORY_ADD -> calculator.memoryAdd();
            case MEMORY_SUBTRACT -> calculator.memorySubtract();
            case SECOND -> second = !second;
            case ANGLE -> setAngles(settings.angles().other());
            case OPEN -> calculator.open();
            case CLOSE -> calculator.close();
            case FACTORIAL -> calculator.function(MathFunction.FACTORIAL);
            case SQUARE -> calculator.function(MathFunction.SQUARE);
            case CUBE -> calculator.function(MathFunction.CUBE);
            case POWER -> calculator.operator(Operator.POWER);
            case SQUARE_ROOT -> calculator.function(MathFunction.SQUARE_ROOT);
            case ROOT -> calculator.operator(Operator.ROOT);
            case RECIPROCAL -> calculator.function(MathFunction.RECIPROCAL);
            case SIN -> trigonometric(MathFunction.SIN);
            case COS -> trigonometric(MathFunction.COS);
            case TAN -> trigonometric(MathFunction.TAN);
            case SINH -> trigonometric(MathFunction.SINH);
            case COSH -> trigonometric(MathFunction.COSH);
            case TANH -> trigonometric(MathFunction.TANH);
            case PI -> calculator.insert(DecimalSystem.PI, new Term.Named("π"));
            case EULER -> calculator.insert(DecimalSystem.E, new Term.Named("e"));
            case LN -> calculator.function(MathFunction.LN);
            case LOG -> calculator.function(MathFunction.LOG);
            case LOG2 -> calculator.function(MathFunction.LOG2);
            case EXP -> calculator.function(MathFunction.EXP);
            case POW10 -> calculator.function(MathFunction.POW10);
            case POW2 -> calculator.function(MathFunction.POW2);
            case ABSOLUTE -> calculator.function(MathFunction.ABSOLUTE);
            case EXPONENT -> calculator.operator(Operator.EXPONENT);
            case MOD -> calculator.operator(Operator.MOD);
            case RANDOM -> {
                BigDecimal number = new BigDecimal(random.getAsDouble()).round(new MathContext(15)).stripTrailingZeros();
                calculator.insert(number, new Term.Number(number));
            }
            case ANSWER -> calculator.insert(calculator.answer(), new Term.Named("Ans"));
            case AND -> calculator.operator(Operator.AND);
            case OR -> calculator.operator(Operator.OR);
            case XOR -> calculator.operator(Operator.XOR);
            case NAND -> calculator.operator(Operator.NAND);
            case NOR -> calculator.operator(Operator.NOR);
            case NOT -> calculator.function(MathFunction.NOT);
            case SHIFT_LEFT -> calculator.operator(Operator.SHIFT_LEFT);
            case SHIFT_RIGHT -> calculator.operator(Operator.SHIFT_RIGHT);
            case ROTATE_LEFT -> calculator.function(MathFunction.ROTATE_LEFT);
            case ROTATE_RIGHT -> calculator.function(MathFunction.ROTATE_RIGHT);
        }
        refresh();
    }
    // --8<-- [end:press]

    /** With 2nd on, sin becomes sin⁻¹; 2nd switches off after one use. */
    private void trigonometric(MathFunction function) {
        calculator.function(second ? function.inverse() : function);
        second = false;
    }

    /**
     * A character typed on the keyboard: digits (A-F in HEX), . and , for the point, + - * / ^ % ( ) = ! and, in
     * Programmer, & | ~ < > for AND, OR, NOT and the shifts.
     *
     * @return false if the character means nothing to the calculator
     */
    public boolean typed(char c) {
        Key key = keyFor(c);
        if (key == null || !enabled(key)) return false;
        press(key);
        return true;
    }

    /** The key a typed character stands for, or null. */
    public Key keyFor(char c) {
        Key digit = Key.forDigit(c);
        if (digit != null) return digit;
        boolean programmer = settings.mode() == Mode.PROGRAMMER;
        return switch (c) {
            case '.', ',' -> Key.POINT;
            case '+' -> Key.ADD;
            case '-', '−' -> Key.SUBTRACT;
            case '*', '×' -> Key.MULTIPLY;
            case '/', '÷', ':' -> Key.DIVIDE;
            case '%' -> programmer ? Key.MOD : Key.PERCENT;
            case '^' -> programmer ? Key.XOR : Key.POWER;
            case '(' -> Key.OPEN;
            case ')' -> Key.CLOSE;
            case '=' -> Key.EQUALS;
            case '!' -> Key.FACTORIAL;
            case '&' -> Key.AND;
            case '|' -> Key.OR;
            case '~' -> Key.NOT;
            case '<' -> Key.SHIFT_LEFT;
            case '>' -> Key.SHIFT_RIGHT;
            case 'p' -> Key.PI;
            default -> null;
        };
    }

    /** True if the key is on the current keypad and does something in the current number base. */
    public boolean enabled(Key key) {
        boolean onKeypad = switch (settings.mode()) {
            case BASIC -> Keypads.BASIC.keys().contains(key);
            case SCIENTIFIC -> Keypads.BASIC.keys().contains(key) || Keypads.SCIENTIFIC.keys().contains(key);
            case PROGRAMMER -> Keypads.PROGRAMMER_LOGIC.keys().contains(key)
                    || Keypads.PROGRAMMER_DIGITS.keys().contains(key);
        };
        if (!onKeypad) return false;
        if (key.kind() == Key.Kind.DIGIT && key != Key.POINT) {
            return calculator.system().radix().accepts(key.digit());
        }
        return true;
    }

    private void addTape(TapeEntry entry) {
        tape.add(entry);
        view.addTape(entry);
    }

    // ---------------------------------------------------------------- settings

    public void setMode(Mode mode) {
        EdtExecutor.requireEdt();
        if (mode == settings.mode()) return;
        boolean otherNumbers = mode.decimal() != settings.mode().decimal();
        save(settings.withMode(mode));
        second = false;
        if (otherNumbers) calculator.changeSystem(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public void setPaperTape(boolean shown) {
        save(settings.withPaperTape(shown));
        view.showSettings(settings);
    }

    public void setGrouping(boolean on) {
        save(settings.withGrouping(on));
        calculator.changeNotation(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public void setAngles(AngleUnit unit) {
        save(settings.withAngles(unit));
        if (settings.mode().decimal()) calculator.changeNotation(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public void setRadix(Radix radix) {
        if (radix == settings.radix()) return;
        save(settings.withRadix(radix));
        calculator.changeNotation(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public void setWordSize(WordSize word) {
        if (word == settings.word()) return;
        save(settings.withWord(word));
        calculator.changeSystem(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    public void setSigned(boolean signed) {
        if (signed == settings.signed()) return;
        save(settings.withSigned(signed));
        calculator.changeSystem(settings.numberSystem(locale));
        view.showSettings(settings);
        refresh();
    }

    private void save(Settings newSettings) {
        settings = newSettings;
        preferences.setSettings(newSettings);
    }

    /** A click on a bit of the bit field. */
    public void flipBit(int bit) {
        if (calculator.system() instanceof ProgrammerSystem programmer && calculator.error().isEmpty()) {
            calculator.enter(programmer.flipBit(calculator.value(), bit));
            refresh();
        }
    }

    // ---------------------------------------------------------------- clipboard

    public void copy() {
        if (calculator.error().isPresent()) {
            view.beep();
            return;
        }
        view.copyToClipboard(calculator.system().copyText(calculator.value()));
    }

    public void paste() {
        Optional<BigDecimal> number = view.clipboardText()
                .flatMap(text -> PastedNumber.parse(text, calculator.system(), locale));
        if (number.isEmpty()) {
            view.beep();
            return;
        }
        calculator.enter(number.get());
        refresh();
    }

    /** The paper tape as text: each calculation, its result and an empty line. */
    public String tapeText() {
        StringBuilder text = new StringBuilder();
        for (TapeEntry entry : tape) {
            text.append(entry.expression()).append('\n').append("= ").append(entry.result()).append("\n\n");
        }
        return text.toString();
    }

    public void copyTape() {
        if (tape.isEmpty()) {
            view.beep();
            return;
        }
        view.copyToClipboard(tapeText());
    }

    public void clearTape() {
        tape.clear();
        view.clearTape();
    }

    // ---------------------------------------------------------------- display

    private void refresh() {
        NumberSystem system = calculator.system();
        Map<Radix, String> radixValues = new EnumMap<>(Radix.class);
        java.math.BigInteger bits = null;
        if (system instanceof ProgrammerSystem programmer) {
            BigDecimal value = calculator.error().isPresent() ? BigDecimal.ZERO : calculator.value();
            for (Radix radix : Radix.values()) radixValues.put(radix, programmer.format(value, radix));
            bits = programmer.pattern(value);
        }
        Optional<String> error = calculator.error();
        view.showDisplay(new DisplayState(calculator.expressionText(),
                error.map(msg::get).orElseGet(calculator::displayText), error.isPresent(), calculator.hasMemory(),
                second, calculator.openBrackets(), radixValues, bits));
    }

    // ---------------------------------------------------------------- program

    public void setLanguage(Optional<Language> language) {
        try {
            locales.setApplicationLanguage(language);
        } catch (IOException e) {
            LOG.warn("Could not save the language", e);
            view.showError(msg.format("error.save-language", e.getMessage()));
            return;
        }
        if (locales.restartRequired()) {
            view.showInfo(Messages.commons(msg.locale()).format("language.restart", "amade-calculator"));
        }
    }

    public void about() {
        view.showAbout();
    }

    public void exit() {
        view.close();
        exit.run();
    }
}
