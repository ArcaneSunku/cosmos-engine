package atomixsoft.dev.cosmos.scene;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;

import java.util.*;

public final class Scene {

    private final Map<EntityId, Entity> m_Entities;

    private String m_Name;
    private EntityId m_PrimaryCameraId;

    public Scene(String name) {
        validateName(name);

        m_Name = name;
        m_PrimaryCameraId = null;

        m_Entities = new LinkedHashMap<>();
    }

    void onComponentRemoved(Entity entity, Class<? extends Component> type) {
        if(type != CameraComponent.class)
            return;

        if(m_PrimaryCameraId == null)
            return;

        if(entity.getId().equals(m_PrimaryCameraId))
            m_PrimaryCameraId = null;
    }

    public void clear() {
        for (Entity entity : List.copyOf(m_Entities.values()))
            entity.invalidate();

        m_PrimaryCameraId = null;
        m_Entities.clear();
    }

    public Entity createEntity() {
        return createEntity("Entity");
    }

    public Entity createEntity(String name) {
        EntityId id;

        do {
            id = EntityId.random();
        } while (m_Entities.containsKey(id));

        return createEntity(id, name);
    }

    public Entity createEntity(EntityId id, String name) {
        if (id == null) throw new IllegalArgumentException("Entity ID cannot be null!");

        validateName(name);
        if (m_Entities.containsKey(id)) throw new IllegalStateException("Scene already contains entity ID: " + id);

        final Entity entity = new Entity(this, id, name);
        m_Entities.put(id, entity);

        return entity;
    }

    public boolean destroyEntity(Entity entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null!");

        final Entity registeredEntity = m_Entities.get(entity.getId());
        if (registeredEntity != entity) return false;

        destroyEntityRecursive(entity);
        return true;
    }

    public boolean destroyEntity(EntityId id) {
        if (id == null) throw new IllegalArgumentException("Entity ID cannot be null!");

        final Entity entity = m_Entities.remove(id);
        if (entity == null) return false;

        destroyEntityRecursive(entity);
        return true;
    }

    public Entity findEntity(EntityId id) {
        if (id == null) throw new IllegalArgumentException("Entity ID cannot be null!");
        return m_Entities.get(id);
    }

    public Scene setName(String name) {
        validateName(name);
        m_Name = name;

        return this;
    }

    public void setPrimaryCamera(Entity entity) {
        if(entity == null)
            throw new IllegalArgumentException("Primary Camera Entity cannot be null!");

        final Entity registeredEntity = m_Entities.get(entity.getId());
        if(registeredEntity != entity)
            throw new IllegalArgumentException("Primary Camera Entity must belong to this Scene!");

        if(!entity.hasComponent(CameraComponent.class))
            throw new IllegalArgumentException("Primary Camera Entity must contain a CameraComponent!");

        m_PrimaryCameraId = entity.getId();
    }

    public void clearPrimaryCamera() {
        m_PrimaryCameraId = null;
    }

    public boolean containsEntity(EntityId id) {
        if (id == null) throw new IllegalArgumentException("Entity ID cannot be null!");
        return m_Entities.containsKey(id);
    }

    public Collection<Entity> getEntities() {
        return Collections.unmodifiableCollection(m_Entities.values());
    }

    public List<Entity> getRootEntities() {
        return m_Entities.values().stream().filter(Entity::isRoot).toList();
    }

    public <T extends Component> List<Entity> getEntitiesWithComponent(Class<T> type) {
        if(type == null)
            throw new IllegalArgumentException("Component type cannot be null!");

        return m_Entities.values().stream().filter(entity -> entity.hasComponent(type)).toList();
    }

    public int getEntityCount() {
        return m_Entities.size();
    }

    public String getName() {
        return m_Name;
    }

    public Entity getPrimaryCameraEntity() {
        if(m_PrimaryCameraId == null)
            return null;

        return m_Entities.get(m_PrimaryCameraId);
    }

    public CameraComponent getPrimaryCameraComponent() {
        final Entity entity = getPrimaryCameraEntity();
        if(entity == null)
            return null;

        return entity.getComponent(CameraComponent.class);
    }

    public boolean hasPrimaryCamera() {
        return m_PrimaryCameraId != null;
    }

    private void destroyEntityRecursive(Entity entity) {
        for(Entity child : List.copyOf(entity.getChildren()))
            destroyEntityRecursive(child);

        if(entity.getId().equals(m_PrimaryCameraId))
            m_PrimaryCameraId = null;

        m_Entities.remove(entity.getId());
        entity.invalidate();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Scene name cannot be empty!");
    }

}