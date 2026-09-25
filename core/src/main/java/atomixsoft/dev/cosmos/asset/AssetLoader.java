package atomixsoft.dev.cosmos.asset;

public interface AssetLoader<T> {

    T load(AssetSource source);
    default void unload(T asset) { }

}
