package atomixsoft.dev.cosmos.asset;

public final class AssetLoadException extends RuntimeException {

    public AssetLoadException(String message) {
        super(message);
    }

    public AssetLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}
