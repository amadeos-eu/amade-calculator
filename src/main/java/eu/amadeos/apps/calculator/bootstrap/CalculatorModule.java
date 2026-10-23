package eu.amadeos.apps.calculator.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.apps.calculator.repository.CalculatorPreferences;
import eu.amadeos.apps.calculator.ui.main.MainPresenter;
import eu.amadeos.apps.calculator.ui.main.MainWindow;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * amade-calculator bindings. Theme, language and the other shared services come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class CalculatorModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.apps.calculator.messages";

    @Override
    protected void configure() {
        bind(CalculatorPreferences.class).in(Scopes.SINGLETON);
        bind(MainWindow.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    MainPresenter presenter(MainWindow window, Messages msg, LocaleSettings locales, CalculatorPreferences preferences,
                            Locale locale) {
        SecureRandom random = new SecureRandom();
        return new MainPresenter(window, msg, locales, preferences, locale, random::nextDouble, () -> System.exit(0));
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(CalculatorModule.class, MESSAGES, locale);
    }
}
