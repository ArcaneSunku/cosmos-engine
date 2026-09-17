package atomixsoft.dev.cosmos.scene;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.spatial.Transform;
import org.joml.Matrix4f;

import java.util.*;

public final class Entity {

    private final EntityId m_Id;
    private final Transform m_Transform;

    private final Map<Class<? extends Component>, Component> m_Components;

    private Scene m_Scene;

    private Entity m_Parent;
    private final List<Entity> m_Children;

    private String m_Name;
    private boolean m_Valid;

    Entity(Scene scene, EntityId id, String name) {
        if(id == null)
            throw new IllegalArgumentException("Entity ID cannot be null!");

        if(scene == null)
            throw new IllegalArgumentException("Scene cannot be null!");

        m_Scene = scene;

        m_Parent = null;
        m_Children = new ArrayList<>();

        validateName(name);

        m_Id = id;
        m_Name = name;

        m_Transform = new Transform();
        m_Components = new LinkedHashMap<>();

        m_Valid = true;
    }

    public void invalidate() {
        if(!m_Valid) return;

        if(m_Parent != null) {
            m_Parent.m_Children.remove(this);
            m_Parent = null;
        }

        for(Entity child : new ArrayList<>(m_Children))
            child.m_Parent = null;

        m_Children.clear();
        m_Components.clear();

        m_Scene = null;
        m_Valid = false;
    }

    public <T extends Component> T addComponent(T component) {
        validate();
        if(component == null)
            throw new IllegalArgumentException("Component cannot be null!");

        final Class<? extends Component> type = component.getClass().asSubclass(Component.class);
        if(m_Components.containsKey(type))
            throw new IllegalStateException("Entity already contains component: " + type.getName());

        m_Components.put(type, component);
        return component;
    }

    public <T extends Component> T removeComponent(Class<T> type) {
        validate();
        validateComponentType(type);

        final Component component = m_Components.remove(type);
        if(component == null)
            return null;

        m_Scene.onComponentRemoved(this, type);
        return type.cast(component);
    }

    public Entity removeParent() {
        return setParent(null);
    }

    public Entity setParent(Entity parent) {
        validate();
        if(parent == this)
            throw new IllegalArgumentException("Entity cannot be its own parent");

        if(parent != null) {
            parent.validate();
            if(parent.m_Scene != m_Scene)
                throw new IllegalArgumentException("Entities must belong to the same Scene!");

            Entity ancestor = parent;
            while(ancestor != null) {
                if(ancestor == this)
                    throw new IllegalArgumentException("Entity hierarchy cannot contain a cycle!");

                ancestor = ancestor.m_Parent;
            }
        }

        if(m_Parent == parent)
            return this;

        if(m_Parent != null)
            m_Parent.m_Children.remove(this);

        m_Parent = parent;
        if(m_Parent != null)
            m_Parent.m_Children.add(this);

        return this;
    }

    public Entity setName(String name) {
        validate();
        validateName(name);

        m_Name = name;
        return this;
    }

    public boolean hasComponent(Class<? extends Component> type) {
        validate();
        validateComponentType(type);

        return m_Components.containsKey(type);
    }

    public boolean isValid() {
        return m_Valid;
    }

    public Entity getParent() {
        validate();
        return m_Parent;
    }

    public List<Entity> getChildren() {
        validate();
        return Collections.unmodifiableList(m_Children);
    }

    public int getChildCount() {
        validate();
        return m_Children.size();
    }

    public boolean isRoot() {
        validate();
        return m_Parent == null;
    }

    public EntityId getId() {
        return m_Id;
    }

    public String getName() {
        return m_Name;
    }

    public Transform getTransform() {
        validate();
        return m_Transform;
    }

    public <T extends Component> T getComponent(Class<T> type) {
        validate();
        validateComponentType(type);

        final Component component = m_Components.get(type);
        if(component == null)
            return null;

        return type.cast(component);
    }

    public Collection<Component> getComponents() {
        validate();
        return Collections.unmodifiableCollection(m_Components.values());
    }

    public Matrix4f getWorldMatrix(Matrix4f destination) {
        validate();
        if(destination == null)
            throw new IllegalArgumentException("Destination Matrix cannot be null!");

        if(m_Parent == null)
            return m_Transform.getMatrix(destination);

        m_Parent.getWorldMatrix(destination);
        return m_Transform.applyTo(destination);
    }

    public Matrix4f getInverseWorldMatrix(Matrix4f destination) {
        return getWorldMatrix(destination).invert();
    }

    private static void validateName(String name) {
        if(name == null || name.isBlank())
            throw new IllegalArgumentException("Entity name cannot be empty!");
    }

    private static void validateComponentType(Class<? extends Component> type) {
        if(type == null)
            throw new IllegalArgumentException("Component type cannot be null!");
    }

    private void validate() {
        if(!m_Valid)
            throw new IllegalStateException("Entity is no longer valid!");
    }

}
