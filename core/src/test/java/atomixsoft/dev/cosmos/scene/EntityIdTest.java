package atomixsoft.dev.cosmos.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityIdTest {

    @Test
    void stringRoundTripPreservesIdentity() {
        final EntityId original = EntityId.random();
        final EntityId parsed = EntityId.fromString(original.toString());

        assertEquals(original, parsed);
    }

}