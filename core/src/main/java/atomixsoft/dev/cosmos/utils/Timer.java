package atomixsoft.dev.cosmos.utils;

public class Timer {

    private static final double NS_PER_SECOND = 1e9;
    private static final double STATS_INTERVAL = 1.0;

    private long m_LastTime;
    private long m_FrameCount;

    private int m_FramesPerSecond;
    private int m_FramesThisInterval;

    private double m_DeltaTime;
    private double m_ElapsedTime;

    private double m_StatsElapsedTime;
    private double m_AverageFrameTime;

    public Timer() {
        reset();
    }

    public void update() {
        final long currentTime = System.nanoTime();

        m_DeltaTime = (currentTime - m_LastTime) / NS_PER_SECOND;
        m_ElapsedTime += m_DeltaTime;

        m_LastTime = currentTime;

        m_FrameCount++;
        m_FramesThisInterval++;

        updateStats();
    }

    public void reset() {
        m_LastTime = System.nanoTime();
        m_FrameCount = 0;

        m_FramesPerSecond = 0;
        m_FramesThisInterval = 0;

        m_DeltaTime = 0.0;
        m_ElapsedTime = 0.0;

        m_StatsElapsedTime = 0.0;
        m_AverageFrameTime = 0.0;
    }

    public double getDeltaTime() {
        return m_DeltaTime;
    }

    public double getFrameTimeMillis() {
        return m_DeltaTime * 1000.0;
    }

    public double getAverageFrameTimeMillis() {
        return m_AverageFrameTime * 1000.0;
    }

    public double getElapsedTime() {
        return m_ElapsedTime;
    }

    public long getFrameCount() {
        return m_FrameCount;
    }

    public int getFramesPerSecond() {
        return m_FramesPerSecond;
    }

    private void updateStats() {
        m_StatsElapsedTime += m_DeltaTime;

        if(m_StatsElapsedTime < STATS_INTERVAL)
            return;

        m_FramesPerSecond = (int) Math.round(m_FramesThisInterval / m_StatsElapsedTime);
        m_AverageFrameTime = m_StatsElapsedTime / m_FramesThisInterval;

        m_FramesThisInterval = 0;
        m_StatsElapsedTime = 0;
    }

}
