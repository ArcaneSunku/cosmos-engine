package atomixsoft.dev.cosmos;

import atomixsoft.dev.cosmos.utils.WindowConfig;

public interface Application {

    WindowConfig getWindowConfig();

    default void initialize(Engine engine) {}
    default void update(Engine engine, double dt) {}
    default void fixedUpdate(Engine engine, double dt) {}
    default void render(Engine engine, double alpha) {}
    default void shutdown(Engine engine) {}

}
