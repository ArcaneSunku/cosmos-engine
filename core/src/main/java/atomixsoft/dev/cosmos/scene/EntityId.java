package atomixsoft.dev.cosmos.scene;

import java.util.UUID;

public record EntityId(UUID id) {

    public EntityId {
        if(id == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");
    }

    public static EntityId random() {
        return new EntityId(UUID.randomUUID());
    }

    public static EntityId fromString(String value) {
        if(value == null || value.isBlank())
            throw new IllegalArgumentException("Entity ID string cannot be empty!");

        return new EntityId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
