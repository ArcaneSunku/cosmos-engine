package atomixsoft.dev.cosmos.asset;

import java.nio.charset.StandardCharsets;

public interface AssetSource {

    byte[] readBytes(String path);

    default String readString(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

}
