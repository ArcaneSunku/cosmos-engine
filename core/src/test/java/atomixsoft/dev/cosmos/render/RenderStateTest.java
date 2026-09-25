package atomixsoft.dev.cosmos.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderStateTest {

    @Test
    void opaquePresetUsesExpectedState() {
        final RenderState state = RenderState.OPAQUE;

        assertTrue(state.isDepthTestEnabled());
        assertTrue(state.isDepthWriteEnabled());

        assertEquals(CullMode.BACK, state.getCullMode());
        assertEquals(BlendMode.NONE, state.getBlendMode());
    }

    @Test
    void transparentPresetUsesExpectedState() {
        final RenderState state = RenderState.TRANSPARENT;

        assertTrue(state.isDepthTestEnabled());
        assertFalse(state.isDepthWriteEnabled());

        assertEquals(CullMode.BACK, state.getCullMode());
        assertEquals(BlendMode.ALPHA, state.getBlendMode());
    }

    @Test
    void nullCullModeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RenderState(true, true, null, BlendMode.NONE));
    }

    @Test
    void nullBlendModeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RenderState(true, true, CullMode.BACK, null));
    }

}