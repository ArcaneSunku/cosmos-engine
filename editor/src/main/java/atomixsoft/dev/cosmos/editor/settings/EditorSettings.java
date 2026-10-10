package atomixsoft.dev.cosmos.editor.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Properties;

public final class EditorSettings {

    private static final String WORKSPACE_LAYOUT_VERSION = "workspace.layoutVersion";

    private static final String SHOW_HIERARCHY = "panel.hierarchy";
    private static final String SHOW_INSPECTOR = "panel.inspector";
    private static final String SHOW_ASSETS = "panel.assets";
    private static final String SHOW_SCENE = "panel.scene";
    private static final String SHOW_GAME = "panel.game";

    private final Path m_Path;

    private int m_WorkspaceLayoutVersion;

    private boolean m_ShowHierarchy;
    private boolean m_ShowInspector;
    private boolean m_ShowAssets;
    private boolean m_ShowScene;
    private boolean m_ShowGame;

    private EditorSettings(Path path) {
        m_Path = path.toAbsolutePath().normalize();
        setDefaults();
    }

    public static EditorSettings load(Path path) {
        if (path == null)
            throw new IllegalArgumentException("Editor Settings path cannot be null!");

        final EditorSettings settings = new EditorSettings(path);
        if (!Files.isRegularFile(settings.m_Path))
            return settings;

        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(settings.m_Path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load Editor Settings: " + settings.m_Path, exception);
        }

        settings.m_WorkspaceLayoutVersion = readInteger(properties, WORKSPACE_LAYOUT_VERSION, 0);

        settings.m_ShowHierarchy = readBoolean(properties, SHOW_HIERARCHY, true);
        settings.m_ShowInspector = readBoolean(properties, SHOW_INSPECTOR, true);
        settings.m_ShowAssets = readBoolean(properties, SHOW_ASSETS, true);
        settings.m_ShowScene = readBoolean(properties, SHOW_SCENE, true);
        settings.m_ShowGame = readBoolean(properties, SHOW_GAME, true);

        return settings;
    }

    public void save() {
        final Path parent = m_Path.getParent();

        try {
            if (parent != null)
                Files.createDirectories(parent);

            final Properties properties = new Properties();
            properties.setProperty(WORKSPACE_LAYOUT_VERSION, Integer.toString(m_WorkspaceLayoutVersion));

            properties.setProperty(SHOW_HIERARCHY, Boolean.toString(m_ShowHierarchy));
            properties.setProperty(SHOW_INSPECTOR, Boolean.toString(m_ShowInspector));
            properties.setProperty(SHOW_ASSETS, Boolean.toString(m_ShowAssets));
            properties.setProperty(SHOW_SCENE, Boolean.toString(m_ShowScene));
            properties.setProperty(SHOW_GAME, Boolean.toString(m_ShowGame));

            try (Writer writer = Files.newBufferedWriter(m_Path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Cosmos Editor Settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save Editor Settings: " + m_Path, exception);
        }
    }

    public int getWorkspaceLayoutVersion() {
        return m_WorkspaceLayoutVersion;
    }

    public void setWorkspaceLayoutVersion(int version) {
        if (version < 0)
            throw new IllegalArgumentException("Workspace layout version cannot be negative!");

        m_WorkspaceLayoutVersion = version;
    }

    public boolean isShowHierarchy() {
        return m_ShowHierarchy;
    }

    public void setShowHierarchy(boolean showHierarchy) {
        m_ShowHierarchy = showHierarchy;
    }

    public boolean isShowInspector() {
        return m_ShowInspector;
    }

    public void setShowInspector(boolean showInspector) {
        m_ShowInspector = showInspector;
    }

    public boolean isShowAssets() {
        return m_ShowAssets;
    }

    public void setShowAssets(boolean showAssets) {
        m_ShowAssets = showAssets;
    }

    public boolean isShowScene() {
        return m_ShowScene;
    }

    public void setShowScene(boolean showScene) {
        m_ShowScene = showScene;
    }

    public boolean isShowGame() {
        return m_ShowGame;
    }

    public void setShowGame(boolean showGame) {
        m_ShowGame = showGame;
    }

    private void setDefaults() {
        m_WorkspaceLayoutVersion = 0;

        m_ShowHierarchy = true;
        m_ShowInspector = true;
        m_ShowAssets = true;
        m_ShowScene = true;
        m_ShowGame = true;
    }

    private static boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        final String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;

        if (value.equalsIgnoreCase("true"))
            return true;

        if (value.equalsIgnoreCase("false"))
            return false;

        return defaultValue;
    }

    private static int readInteger(Properties properties, String key, int defaultValue) {
        final String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

}