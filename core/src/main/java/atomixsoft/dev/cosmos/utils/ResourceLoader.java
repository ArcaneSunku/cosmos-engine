package atomixsoft.dev.cosmos.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceLoader {

    private ResourceLoader() {}

    public static String readString(String resourcePath) {
        if(resourcePath == null || resourcePath.isBlank())
            throw new IllegalArgumentException("Resource path cannot be empty!");

        try(InputStream is = ResourceLoader.class.getResourceAsStream(resourcePath)) {
            if(is == null)
                throw new IllegalArgumentException("Resource does not exist: " + resourcePath);

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + resourcePath, e);
        }
    }

}
