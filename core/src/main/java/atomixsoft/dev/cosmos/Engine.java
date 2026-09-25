package atomixsoft.dev.cosmos;

import atomixsoft.dev.cosmos.asset.AssetManager;
import atomixsoft.dev.cosmos.input.Input;
import atomixsoft.dev.cosmos.render.*;
import atomixsoft.dev.cosmos.utils.Timer;
import atomixsoft.dev.cosmos.utils.WindowConfig;

public class Engine {

    private static final double FIXED_TIMESTEP = 1.0 / 60.0;
    private static final double MAX_FRAME_DELTA = 0.25;

    private final Application m_Application;
    private final AssetManager m_Assets;
    private final Graphics m_Graphics;
    private final Input m_Input;
    private final Timer m_Timer;
    private final Window m_Window;

    private boolean m_Initialized;
    private boolean m_Running;

    public Engine(Application application) {
        if(application == null)
            throw new IllegalArgumentException("Application cannot be null!");

        m_Application = application;

        final WindowConfig config = m_Application.getWindowConfig();
        if(config == null)
            throw new IllegalStateException("Application returned a null Window Config!");

        m_Assets = new AssetManager();
        m_Graphics = new Graphics();
        m_Input = new Input();
        m_Timer = new Timer();
        m_Window = new Window(config);

        m_Initialized = false;
        m_Running = false;
    }

    public void initialize() {
        if(m_Initialized) return;

        boolean appStarted = false;

        try {
            m_Window.create();

            m_Input.initialize(m_Window.getHandle());

            m_Graphics.initialize();
            m_Graphics.resize(m_Window.getFramebufferWidth(),  m_Window.getFramebufferHeight());

            m_Initialized = true;
            appStarted = true;

            m_Application.initialize(this);
        } catch (RuntimeException | Error e) {
            Throwable failure = e;

            if(appStarted)
                failure = captureFailure(failure, () -> m_Application.shutdown(this));

            failure = disposeSystems(failure);

            m_Initialized = false;
            m_Running = false;

            throw propagateFailure(failure);
        }
    }

    public void run() {
        validate();

        if(m_Running)
            throw new IllegalStateException("Engine is already running!");

        m_Timer.reset();
        double accumulator = 0.0;

        m_Running = true;
        Throwable failure = null;

        try {
            while(m_Running) {
                m_Input.beginFrame();
                m_Window.update();

                if(m_Window.shouldClose()) {
                    stop();
                    continue;
                }

                if(m_Window.hasFramebufferResized()) {
                    m_Graphics.resize(m_Window.getFramebufferWidth(),  m_Window.getFramebufferHeight());
                    m_Window.clearFramebufferResized();
                }

                m_Timer.update();

                final double frameDeltaTime = Math.min(m_Timer.getDeltaTime(), MAX_FRAME_DELTA);
                accumulator += frameDeltaTime;

                m_Application.update(this, frameDeltaTime);
                while(accumulator >= FIXED_TIMESTEP) {
                    m_Application.fixedUpdate(this, FIXED_TIMESTEP);
                    accumulator -= FIXED_TIMESTEP;
                }

                final double interpolation = accumulator / FIXED_TIMESTEP;
                m_Graphics.clear();
                m_Application.render(this, interpolation);

                m_Window.swapBuffers();
            }
        } catch(RuntimeException | Error e) {
            failure = e;
        }

        failure = captureFailure(failure, this::shutdown);
        if(failure != null)
            throw propagateFailure(failure);
    }

    public void stop() {
        m_Running = false;
    }

    public void shutdown() {
        if(!m_Initialized) return;
        Throwable failure = null;

        failure = captureFailure(failure, () -> m_Application.shutdown(this));
        failure = disposeSystems(failure);

        m_Running = false;
        m_Initialized = false;

        if(failure != null)
            throw propagateFailure(failure);
    }

    private Throwable disposeSystems(Throwable failure) {
        failure = captureFailure(failure, m_Assets::clear);
        failure = captureFailure(failure, m_Graphics::dispose);
        failure = captureFailure(failure, m_Input::dispose);
        failure = captureFailure(failure, m_Window::close);

        return failure;
    }

    private static Throwable captureFailure(Throwable failure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error e) {
            if(failure == null)
                return e;

            failure.addSuppressed(e);
        }

        return failure;
    }

    private static RuntimeException propagateFailure(Throwable failure) {
        if(failure instanceof RuntimeException runtimeException)
            return runtimeException;

        if(failure instanceof Error error)
            throw error;

        return new IllegalStateException("Unexpected Engine lifecycle failure!", failure);
    }

    public AssetManager getAssets() {
        validate();
        return m_Assets;
    }

    public Graphics getGraphics() {
        validate();
        return m_Graphics;
    }

    public Input getInput() {
        validate();
        return m_Input;
    }

    public double getDeltaTime() {
        validate();
        return m_Timer.getDeltaTime();
    }

    public double getFrameTimeMilliseconds() {
        validate();
        return m_Timer.getFrameTimeMillis();
    }

    public double getAverageFrameTimeMilliseconds() {
        validate();
        return m_Timer.getAverageFrameTimeMillis();
    }

    public double getElapsedTime() {
        validate();
        return m_Timer.getElapsedTime();
    }

    public long getFrameCount() {
        validate();
        return m_Timer.getFrameCount();
    }

    public int getFramesPerSecond() {
        validate();
        return m_Timer.getFramesPerSecond();
    }

    private void validate() {
        if(!m_Initialized)
            throw new IllegalStateException("Engine has not been initialized!");
    }

}
