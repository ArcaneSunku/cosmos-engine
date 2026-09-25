package atomixsoft.dev.cosmos.render;

public final class RenderState {

    public static final RenderState OPAQUE = new RenderState(true, true, CullMode.BACK, BlendMode.NONE);
    public static final RenderState TRANSPARENT = new RenderState(true, false, CullMode.BACK, BlendMode.ALPHA);

    private final boolean m_DepthTestEnabled;
    private final boolean m_DepthWriteEnabled;

    private final CullMode m_CullMode;
    private final BlendMode m_BlendMode;

    public RenderState(boolean depthTest, boolean depthWrite, CullMode cullMode, BlendMode blendMode) {
        if(cullMode == null)
            throw new IllegalArgumentException("Cull Mode cannot be null!");

        if(blendMode == null)
            throw new IllegalArgumentException("Blend Mode cannot be null!");

        m_DepthTestEnabled = depthTest;
        m_DepthWriteEnabled = depthWrite;

        m_CullMode = cullMode;
        m_BlendMode = blendMode;
    }

    public boolean isDepthTestEnabled() {
        return m_DepthTestEnabled;
    }

    public boolean isDepthWriteEnabled() {
        return m_DepthWriteEnabled;
    }

    public boolean isBlendingEnabled() {
        return m_BlendMode != BlendMode.NONE;
    }

    public CullMode getCullMode() {
        return m_CullMode;
    }

    public BlendMode getBlendMode() {
        return m_BlendMode;
    }

}
