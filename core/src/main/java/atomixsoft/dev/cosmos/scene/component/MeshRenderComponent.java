package atomixsoft.dev.cosmos.scene.component;

import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.scene.Component;

public final class MeshRenderComponent implements Component {

    private Mesh m_Mesh;
    private Material m_Material;

    public MeshRenderComponent(Mesh mesh, Material material) {
        setMesh(mesh);
        setMaterial(material);
    }

    public MeshRenderComponent setMesh(Mesh mesh) {
        if (mesh == null)
            throw new IllegalArgumentException("Mesh cannot be null!");

        m_Mesh = mesh;
        return this;
    }

    public MeshRenderComponent setMaterial(Material material) {
        if (material == null)
            throw new IllegalArgumentException("Material cannot be null!");

        m_Material = material;
        return this;
    }

    public Mesh getMesh() {
        return m_Mesh;
    }

    public Material getMaterial() {
        return m_Material;
    }

}