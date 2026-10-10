package atomixsoft.dev.cosmos.editor.settings;

import java.nio.file.Path;
import java.util.Locale;

public final class EditorPaths {

    private static final String APPLICATION_DIRECTORY = "cosmos";
    private static final String EDITOR_DIRECTORY = "editor";
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String IMGUI_FILE = "workspace.ini";

    private EditorPaths() {

    }

    public static Path getEditorConfigDirectory() {
        return getApplicationConfigDirectory().resolve(EDITOR_DIRECTORY);
    }

    public static Path getSettingsPath() {
        return getEditorConfigDirectory().resolve(SETTINGS_FILE);
    }

    public static Path getImGuiPath() {
        return getEditorConfigDirectory().resolve(IMGUI_FILE);
    }

    private static Path getApplicationConfigDirectory() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String userHome = System.getProperty("user.home");

        if (userHome == null || userHome.isBlank())
            throw new IllegalStateException("Cannot determine the user's home directory!");

        if (osName.contains("win")) {
            final String appData = System.getenv("APPDATA");

            if (appData != null && !appData.isBlank())
                return Path.of(appData, "Cosmos");

            return Path.of(userHome, "AppData", "Roaming", "Cosmos");
        }

        if (osName.contains("mac"))
            return Path.of(userHome, "Library", "Application Support", "Cosmos");

        final String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfigHome != null && !xdgConfigHome.isBlank())
            return Path.of(xdgConfigHome, APPLICATION_DIRECTORY);

        return Path.of(userHome, ".config", APPLICATION_DIRECTORY);
    }

}
