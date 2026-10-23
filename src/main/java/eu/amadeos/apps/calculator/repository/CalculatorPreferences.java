package eu.amadeos.apps.calculator.repository;

import com.google.inject.Inject;
import eu.amadeos.apps.calculator.domain.AngleUnit;
import eu.amadeos.apps.calculator.domain.Mode;
import eu.amadeos.apps.calculator.domain.Radix;
import eu.amadeos.apps.calculator.domain.Settings;
import eu.amadeos.apps.calculator.domain.WordSize;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Settings of amade-calculator in {@code ~/.config/amadeos/calculator/calculator.properties}. Each change is
 * written at once; unknown or broken values fall back to the defaults.
 */
public class CalculatorPreferences {
    public static final String FILE = "calculator.properties";

    private final Path file;
    private final Properties properties;

    @Inject
    public CalculatorPreferences(AppDirectories directories) {
        this(directories.configFile(FILE));
    }

    public CalculatorPreferences(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.properties = PropertiesFile.load(file);
    }

    public Settings settings() {
        Settings d = Settings.DEFAULT;
        Mode mode = Mode.forKey(properties.getProperty("mode", "")).orElse(d.mode());
        AngleUnit angles = "radians".equals(properties.getProperty("angles")) ? AngleUnit.RADIANS
                : "degrees".equals(properties.getProperty("angles")) ? AngleUnit.DEGREES : d.angles();
        Radix radix = Radix.forName(properties.getProperty("radix", "")).orElse(d.radix());
        WordSize word = d.word();
        try {
            word = WordSize.forBits(Integer.parseInt(properties.getProperty("word-size", "").strip())).orElse(word);
        } catch (NumberFormatException e) {
            // keep the default
        }
        return new Settings(mode, flag("paper-tape", d.paperTape()), flag("grouping", d.grouping()), angles, radix,
                word, flag("signed", d.signed()));
    }

    private boolean flag(String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : !"false".equals(value.strip());
    }

    public void setSettings(Settings settings) {
        properties.setProperty("mode", settings.mode().key());
        properties.setProperty("paper-tape", String.valueOf(settings.paperTape()));
        properties.setProperty("grouping", String.valueOf(settings.grouping()));
        properties.setProperty("angles", settings.angles().name().toLowerCase(Locale.ROOT));
        properties.setProperty("radix", settings.radix().name().toLowerCase(Locale.ROOT));
        properties.setProperty("word-size", String.valueOf(settings.word().bits()));
        properties.setProperty("signed", String.valueOf(settings.signed()));
        PropertiesFile.store(file, properties, "amade-calculator");
    }
}
