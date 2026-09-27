package atomixsoft.dev.cosmos.asset.definition;

import atomixsoft.dev.cosmos.asset.AssetKey;
import atomixsoft.dev.cosmos.asset.AssetLoadException;
import atomixsoft.dev.cosmos.render.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class MaterialDefinitionParser {

    private MaterialDefinitionParser() { }

    public static MaterialDefinition parse(String source) {
        if (source == null)
            throw new IllegalArgumentException("Material definition source cannot be null!");

        final Properties properties = new Properties();
        try {
            properties.load(new StringReader(source));
        } catch (IOException e) {
            throw new AssetLoadException("Failed to parse Material definition!", e);
        }

        final String shaderId = require(properties, "shader");

        final boolean depthTest = getBoolean(properties, "render.depthTest", true);
        final boolean depthWrite = getBoolean(properties, "render.depthWrite", true);

        final CullMode cullMode = getEnum(properties, "render.cull", CullMode.class, CullMode.BACK);
        final BlendMode blendMode = getEnum(properties, "render.blend", BlendMode.class, BlendMode.NONE);

        final Map<String, Float> floats = new LinkedHashMap<>();
        final Map<String, MaterialDefinition.Float4Value> float4s = new LinkedHashMap<>();
        final Map<String, MaterialDefinition.TextureReference> textures = new LinkedHashMap<>();

        for (String key : properties.stringPropertyNames().stream().sorted().toList()) {
            if (key.startsWith("float.")) {
                final String uniform = requireUniformName(key, "float.");
                floats.put(uniform, parseFloat(properties.getProperty(key), key));

                continue;
            }

            if (key.startsWith("float4.")) {
                final String uniform = requireUniformName(key, "float4.");
                float4s.put(uniform, parseFloat4(properties.getProperty(key), key));

                continue;
            }

            if (key.startsWith("texture.") && key.endsWith(".asset")) {
                final String uniform = key.substring("texture.".length(), key.length() - ".asset".length());
                if (uniform.isBlank())
                    throw new AssetLoadException("Texture uniform name cannot be empty: " + key);

                final String assetId = require(properties, key);
                final String slotKey = "texture." + uniform + ".slot";
                final int slot = parseInteger(require(properties, slotKey), slotKey);

                textures.put(uniform, new MaterialDefinition.TextureReference(AssetKey.of(Texture2D.class, assetId), slot));
            }
        }

        return new MaterialDefinition(AssetKey.of(Shader.class, shaderId), new RenderState(depthTest, depthWrite, cullMode, blendMode), floats, float4s, textures);
    }

    private static String require(Properties properties, String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank())
            throw new AssetLoadException("Material property is required: " + key);

        return value.trim();
    }

    private static String requireUniformName(String key, String prefix) {
        final String name = key.substring(prefix.length());

        if (name.isBlank())
            throw new AssetLoadException("Material uniform name cannot be empty: " + key);

        return name;
    }

    private static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        final String value = properties.getProperty(key);
        if (value == null)
            return defaultValue;

        if (value.equalsIgnoreCase("true"))
            return true;

        if (value.equalsIgnoreCase("false"))
            return false;

        throw new AssetLoadException("Material property " + key + " must be true or false!");
    }

    private static float parseFloat(String value, String key) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new AssetLoadException("Material property " + key + " must be a float!", e);
        }
    }

    private static int parseInteger(String value, String key) {
        try {
            final int result = Integer.parseInt(value.trim());
            if (result < 0)
                throw new AssetLoadException("Material property " + key + " cannot be negative!");

            return result;
        } catch (NumberFormatException e) {
            throw new AssetLoadException("Material property " + key + " must be an integer!", e);
        }
    }

    private static MaterialDefinition.Float4Value parseFloat4(String value, String key) {
        final String[] parts = value.split(",");

        if (parts.length != 4)
            throw new AssetLoadException("Material property " + key + " must contain four comma-separated floats!");

        return new MaterialDefinition.Float4Value(parseFloat(parts[0], key), parseFloat(parts[1], key), parseFloat(parts[2], key), parseFloat(parts[3], key));
    }

    private static <E extends Enum<E>> E getEnum(Properties properties, String key, Class<E> type, E defaultValue) {
        final String value = properties.getProperty(key);

        if (value == null)
            return defaultValue;

        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AssetLoadException("Invalid value for Material property " + key + ": " + value, e);
        }
    }

}