package atomixsoft.dev.cosmos.asset;

import java.util.Objects;

public final class AssetKey<T> {

    private final String m_Id;
    private final Class<T> m_Type;

    private AssetKey(Class<T> type, String id) {
        if(type == null)
            throw new IllegalArgumentException("Asset type cannot be null!");

        if(id == null || id.isBlank())
            throw new IllegalArgumentException("Asset ID cannot be empty!");

        m_Type = type;
        m_Id = normalizeId(id);
    }

    public String getId() {
        return m_Id;
    }

    public Class<T> getType() {
        return m_Type;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        if(!(obj instanceof AssetKey<?> other))
            return false;

        return m_Id.equals(other.m_Id) && m_Type.equals(other.m_Type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_Id, m_Type);
    }

    @Override
    public String toString() {
        return m_Id;
    }

    public static <T> AssetKey<T> of(Class<T> type, String id) {
        return new AssetKey<>(type, id);
    }

    private static String normalizeId(String id) {
        return id.trim().replace('\\', '/');
    }

}
