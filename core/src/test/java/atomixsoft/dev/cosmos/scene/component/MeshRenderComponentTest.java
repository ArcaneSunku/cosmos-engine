package atomixsoft.dev.cosmos.scene.component;

import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;
import atomixsoft.dev.cosmos.render.Shader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MeshRenderComponentTest {

    @Test
    void storesMeshAndMaterialReferences() {
        final Mesh mesh = new Mesh();
        final Shader shader = new Shader();
        final Material material = new Material(shader);
        final MeshRenderComponent component = new MeshRenderComponent(mesh, material);

        assertSame(mesh, component.getMesh());
        assertSame(material, component.getMaterial());
    }

    @Test
    void nullMeshIsRejected() {
        final Material material = new Material(new Shader());

        assertThrows(IllegalArgumentException.class, () -> new MeshRenderComponent(null, material));
    }

    @Test
    void nullMaterialIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MeshRenderComponent(new Mesh(), null));
    }

}
