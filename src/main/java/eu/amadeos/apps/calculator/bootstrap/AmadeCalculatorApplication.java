package eu.amadeos.apps.calculator.bootstrap;

import eu.amadeos.apps.calculator.ui.main.MainPresenter;
import eu.amadeos.apps.calculator.ui.main.MainWindow;
import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;

public final class AmadeCalculatorApplication {
    public static final AppInfo APP = new AppInfo("amade-calculator", "Calculator", "calculator");

    private AmadeCalculatorApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeCalculatorApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("--version") || args[0].equals("-v"))) {
            System.out.println("amade-calculator " + version());
            return;
        }
        AmadeApplication.launch(APP, injector -> {
            MainWindow window = injector.getInstance(MainWindow.class);
            MainPresenter presenter = injector.getInstance(MainPresenter.class);
            window.bind(presenter);
            presenter.start();
            window.setVisible(true);
        }, new CalculatorModule());
    }
    // --8<-- [end:main]
}
