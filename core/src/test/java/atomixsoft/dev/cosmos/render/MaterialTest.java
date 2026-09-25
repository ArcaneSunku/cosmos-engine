package atomixsoft.dev.cosmos.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaterialTest {

    @Test
    void opaqueRenderStateIsDefault() {
        final Material material = new Material(new Shader());
        assertSame(RenderState.OPAQUE, material.getRenderState());
    }

    @Test
    void renderStateCanBeChanged() {
        final Material material = new Material(new Shader());
        material.setRenderState(RenderState.TRANSPARENT);

        assertSame(RenderState.TRANSPARENT, material.getRenderState());
    }

    @Test
    void sameTextureUniformCanBeReassigned() {
        final Material material = new Material(new Shader());
        final Texture2D first = new Texture2D();
        final Texture2D second = new Texture2D();

        assertDoesNotThrow(() -> material.setTexture("u_Albedo", first, 0).setTexture("u_Albedo", second, 0));
    }

    @Test
    void differentUniformsCannotShareTextureSlot() {
        final Material material = new Material(new Shader());
        material.setTexture("u_Albedo", new Texture2D(), 0);

        assertThrows(IllegalArgumentException.class, () -> material.setTexture("u_Normal", new Texture2D(), 0));
    }

    @Test
    void nullShaderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Material(null));
    }

    @Test
    void nullRenderStateIsRejected() {
        final Material material = new Material(new Shader());
        assertThrows(IllegalArgumentException.class, () -> material.setRenderState(null));
    }

}
