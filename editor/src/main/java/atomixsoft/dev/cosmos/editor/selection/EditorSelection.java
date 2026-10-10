package atomixsoft.dev.cosmos.editor.selection;

import atomixsoft.dev.cosmos.scene.Entity;

public final class EditorSelection {

    private Entity m_Entity;

    public EditorSelection() {
        m_Entity = null;
    }

    public void select(Entity entity) {
        if (entity == null) {
            clear();
            return;
        }

        if (!entity.isValid())
            throw new IllegalArgumentException("Cannot select an invalid Entity!");

        m_Entity = entity;
    }

    public void clear() {
        m_Entity = null;
    }

    public void validate() {
        if (m_Entity != null && !m_Entity.isValid())
            m_Entity = null;
    }

    public boolean isSelected(Entity entity) {
        validate();
        return m_Entity == entity;
    }

    public boolean hasSelection() {
        validate();
        return m_Entity != null;
    }

    public Entity getEntity() {
        validate();
        return m_Entity;
    }

}