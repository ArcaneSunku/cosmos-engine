package atomixsoft.dev.cosmos.asset;

public interface AssetLoader<T> {

    T load(AssetLoadContext context);
    default void unload(T asset) { }

}
