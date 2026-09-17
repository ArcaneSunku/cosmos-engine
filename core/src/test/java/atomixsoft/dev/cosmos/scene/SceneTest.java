package atomixsoft.dev.cosmos.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneTest {

    @Test
    void createdEntitiesHaveUniqueIdentity() {
        final Scene scene = new Scene("Test");

        final Entity first = scene.createEntity("First");
        final Entity second = scene.createEntity("Second");

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2, scene.getEntityCount());
    }

    @Test
    void entityCanBeLookedUpById() {
        final Scene scene = new Scene("Test");
        final Entity entity = scene.createEntity("Entity");

        assertSame(entity, scene.findEntity(entity.getId()));
    }

    @Test
    void duplicateEntityIdsAreRejected() {
        final Scene scene = new Scene("Test");

        final EntityId id = EntityId.random();
        scene.createEntity(id, "First");

        assertThrows(IllegalStateException.class, () -> scene.createEntity(id, "Second"));
    }

    @Test
    void destructionInvalidatesEntity() {
        final Scene scene = new Scene("Test");
        final Entity entity = scene.createEntity("Entity");

        assertTrue(scene.destroyEntity(entity));
        assertFalse(entity.isValid());
        assertNull(scene.findEntity(entity.getId()));
        assertThrows(IllegalStateException.class, entity::getTransform);
    }

    @Test
    void componentsCanBeAddedRetrievedAndRemoved() {
        final Scene scene = new Scene("Test");
        final Entity entity = scene.createEntity("Entity");
        final TestComponent component = entity.addComponent(new TestComponent(42));

        assertTrue(entity.hasComponent(TestComponent.class));
        assertSame(component, entity.getComponent(TestComponent.class));
        assertEquals(42, entity.getComponent(TestComponent.class).value());
        assertSame(component, entity.removeComponent(TestComponent.class));
        assertFalse(entity.hasComponent(TestComponent.class));
    }

    @Test
    void duplicateComponentTypesAreRejected() {
        final Scene scene = new Scene("Test");

        final Entity entity = scene.createEntity("Entity");
        entity.addComponent(new TestComponent(1));

        assertThrows(IllegalStateException.class, () -> entity.addComponent(new TestComponent(2)));
    }

    @Test
    void entityCollectionCannotBeModifiedExternally() {
        final Scene scene = new Scene("Test");
        scene.createEntity("Entity");

        assertThrows(UnsupportedOperationException.class, () -> scene.getEntities().clear());
    }

    @Test
    void entitiesCanBeQueriedByComponent() {
        final Scene scene = new Scene("Test");
        final Entity first = scene.createEntity("First");
        final Entity second = scene.createEntity("Second");
        final Entity third = scene.createEntity("Third");

        first.addComponent(new TestComponent(1));
        third.addComponent(new TestComponent(3));

        final var entities = scene.getEntitiesWithComponent(TestComponent.class);

        assertEquals(2, entities.size());
        assertTrue(entities.contains(first));
        assertFalse(entities.contains(second));
        assertTrue(entities.contains(third));
    }

    @Test
    void componentQueryCannotModifySceneMembership() {
        final Scene scene = new Scene("Test");
        final Entity entity = scene.createEntity("Entity");

        entity.addComponent(new TestComponent(42));

        final var result = scene.getEntitiesWithComponent(TestComponent.class);

        assertThrows(UnsupportedOperationException.class, result::clear);
        assertEquals(1, scene.getEntityCount());
    }

    @Test
    void entityNamesDoNotNeedToBeUnique() {
        final Scene scene = new Scene("Test");
        final Entity first = scene.createEntity("Cube");
        final Entity second = scene.createEntity("Cube");

        assertEquals("Cube", first.getName());
        assertEquals("Cube", second.getName());
        assertNotEquals(first.getId(), second.getId());
    }

    private record TestComponent(int value) implements Component {
    }

}