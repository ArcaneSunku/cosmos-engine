package atomixsoft.dev.cosmos.runtime;

import atomixsoft.dev.cosmos.Application;
import atomixsoft.dev.cosmos.Engine;
import atomixsoft.dev.cosmos.utils.WindowConfig;

public class Runtime implements Application {

    @Override
    public WindowConfig getWindowConfig() {
        return new WindowConfig("Test Environment", 1280, 720);
    }

    static void main(String[] args) {
        final Runtime runtime = new Runtime();
        final Engine engine = new Engine(runtime);

        engine.initialize();
        engine.run();
    }

}
