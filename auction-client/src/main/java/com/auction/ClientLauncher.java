package com.auction;

/**
 * Plain Java entry point used by jpackage.
 *
 * Keeping the launcher separate from the JavaFX Application class avoids the
 * Java launcher trying to resolve JavaFX exclusively from the module path.
 */
public final class ClientLauncher {
    private ClientLauncher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
