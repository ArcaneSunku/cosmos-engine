package atomixsoft.dev.cosmos.asset.definition;

import atomixsoft.dev.cosmos.asset.AssetLoadException;
import atomixsoft.dev.cosmos.render.BlendMode;
import atomixsoft.dev.cosmos.render.CullMode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaterialDefinitionParserTest {

    @Test
    void parsesCompleteMaterialDefinition() {
        final String source = """
                shader=test/shaders/basic
                
                render.depthTest=true
                render.depthWrite=false
                render.cull=BACK
                render.blend=ALPHA
                
                float.u_Value=0.5
                float4.u_Tint=1.0,0.5,0.25,0.75
                
                texture.u_Albedo.asset=test/textures/albedo
                texture.u_Albedo.slot=2
                """;

        final MaterialDefinition definition = MaterialDefinitionParser.parse(source);

        assertEquals("test/shaders/basic", definition.getShader().getId());
        assertTrue(definition.getRenderState().isDepthTestEnabled());
        assertFalse(definition.getRenderState().isDepthWriteEnabled());
        assertEquals(CullMode.BACK, definition.getRenderState().getCullMode());
        assertEquals(BlendMode.ALPHA, definition.getRenderState().getBlendMode());
        assertEquals(0.5f, definition.getFloats().get("u_Value"));

        final MaterialDefinition.Float4Value tint = definition.getFloat4s().get("u_Tint");

        assertEquals(1.0f, tint.x());
        assertEquals(0.75f, tint.w());

        final MaterialDefinition.TextureReference texture = definition.getTextures().get("u_Albedo");

        assertEquals("test/textures/albedo", texture.asset().getId());
        assertEquals(2, texture.slot());
    }

    @Test
    void unknownPropertyIsRejected() {
        assertThrows(AssetLoadException.class, () -> MaterialDefinitionParser.parse("""
                shader=test/shader
                render.depthWrit=false
                """));
    }

    @Test
    void textureWithoutSlotIsRejected() {
        assertThrows(AssetLoadException.class, () -> MaterialDefinitionParser.parse("""
                shader=test/shader
                texture.u_Albedo.asset=test/albedo
                """));
    }

    @Test
    void duplicateTextureSlotsAreRejected() {
        assertThrows(AssetLoadException.class, () -> MaterialDefinitionParser.parse("""
                shader=test/shader
                
                texture.u_First.asset=test/first
                texture.u_First.slot=0
                
                texture.u_Second.asset=test/second
                texture.u_Second.slot=0
                """));
    }

}