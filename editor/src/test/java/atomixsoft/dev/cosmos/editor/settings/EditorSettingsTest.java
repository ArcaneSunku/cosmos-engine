package atomixsoft.dev.cosmos.editor.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EditorSettingsTest {

    @TempDir
    Path m_TempDirectory;

    @Test
    void missingSettingsUseDefaults() {
        final EditorSettings settings = EditorSettings.load(m_TempDirectory.resolve("settings.properties"));

        assertEquals(0, settings.getWorkspaceLayoutVersion());
        assertTrue(settings.isShowHierarchy());
        assertTrue(settings.isShowInspector());
        assertTrue(settings.isShowAssets());
        assertTrue(settings.isShowScene());
        assertTrue(settings.isShowGame());
    }

    @Test
    void settingsRoundTrip() {
        final Path path = m_TempDirectory.resolve("settings.properties");
        final EditorSettings original = EditorSettings.load(path);

        original.setWorkspaceLayoutVersion(4);
        original.setShowHierarchy(false);
        original.setShowGame(false);
        original.save();

        final EditorSettings loaded = EditorSettings.load(path);

        assertEquals(4, loaded.getWorkspaceLayoutVersion());
        assertFalse(loaded.isShowHierarchy());
        assertTrue(loaded.isShowInspector());
        assertTrue(loaded.isShowAssets());
        assertTrue(loaded.isShowScene());
        assertFalse(loaded.isShowGame());
    }

    @Test
    void saveCreatesParentDirectories() {
        final Path path = m_TempDirectory.resolve("nested").resolve("editor").resolve("settings.properties");
        final EditorSettings settings = EditorSettings.load(path);
        settings.save();

        assertTrue(Files.isRegularFile(path));
    }

    @Test
    void malformedValuesFallBackToDefaults() throws Exception {
        final Path path = m_TempDirectory.resolve("settings.properties");
        Files.writeString(path, """
                workspace.layoutVersion=banana
                panel.hierarchy=maybe
                panel.inspector=false
                """);

        final EditorSettings settings = EditorSettings.load(path);

        assertEquals(0, settings.getWorkspaceLayoutVersion());
        assertTrue(settings.isShowHierarchy());
        assertFalse(settings.isShowInspector());
    }

}