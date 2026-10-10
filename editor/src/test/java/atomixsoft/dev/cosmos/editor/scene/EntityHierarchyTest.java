package atomixsoft.dev.cosmos.editor.scene;

import atomixsoft.dev.cosmos.scene.Entity;
import atomixsoft.dev.cosmos.scene.Scene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityHierarchyTest {

    @Test
    void rootEntitiesCanBeReordered() {
        final Scene scene = new Scene("Test");

        final Entity a = scene.createEntity("A");
        final Entity b = scene.createEntity("B");
        final Entity c = scene.createEntity("C");

        c.setSiblingIndex(0);

        assertEquals(List.of(c, a, b), scene.getRootEntities());
    }

    @Test
    void childEntitiesCanBeReordered() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");

        final Entity a = scene.createEntity("A");
        final Entity b = scene.createEntity("B");
        final Entity c = scene.createEntity("C");

        a.setParent(parent);
        b.setParent(parent);

        c.setParent(parent);
        c.setSiblingIndex(0);

        assertEquals(List.of(c, a, b), parent.getChildren());
    }

    @Test
    void reparentCanChooseSiblingIndex() {
        final Scene scene = new Scene("Test");
        final Entity parent = scene.createEntity("Parent");

        final Entity a = scene.createEntity("A");
        final Entity b = scene.createEntity("B");
        final Entity c = scene.createEntity("C");

        a.setParent(parent);
        c.setParent(parent);
        b.setParent(parent, 1);

        assertEquals(List.of(a, b, c), parent.getChildren());
    }

}
