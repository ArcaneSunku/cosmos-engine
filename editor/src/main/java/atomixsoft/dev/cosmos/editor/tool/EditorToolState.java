package atomixsoft.dev.cosmos.editor.tool;

public final class EditorToolState {

    private static final float DEFAULT_TRANSLATION_SNAP = 0.5f;
    private static final float DEFAULT_ROTATION_SNAP = 15.0f;
    private static final float DEFAULT_SCALE_SNAP = 0.1f;

    private TransformTool m_TransformTool;
    private TransformSpace m_TransformSpace;

    private boolean m_SnapEnabled;

    private float m_TranslationSnap;
    private float m_RotationSnap;
    private float m_ScaleSnap;

    public EditorToolState() {
        m_TransformTool = TransformTool.TRANSLATE;
        m_TransformSpace = TransformSpace.LOCAL;

        m_SnapEnabled = false;

        m_TranslationSnap = DEFAULT_TRANSLATION_SNAP;
        m_RotationSnap = DEFAULT_ROTATION_SNAP;
        m_ScaleSnap = DEFAULT_SCALE_SNAP;
    }

    public void setTransformTool(TransformTool tool) {
        if (tool == null)
            throw new IllegalArgumentException("Transform Tool cannot be null!");

        m_TransformTool = tool;
    }

    public void setTransformSpace(TransformSpace space) {
        if (space == null)
            throw new IllegalArgumentException("Transform Space cannot be null!");

        m_TransformSpace = space;
    }

    public void toggleTransformSpace() {
        m_TransformSpace = m_TransformSpace == TransformSpace.LOCAL ? TransformSpace.WORLD : TransformSpace.LOCAL;
    }

    public void setSnapEnabled(boolean enabled) {
        m_SnapEnabled = enabled;
    }

    public void toggleSnap() {
        m_SnapEnabled = !m_SnapEnabled;
    }

    public void setTranslationSnap(float snap) {
        validateSnap(snap);
        m_TranslationSnap = snap;
    }

    public void setRotationSnap(float snap) {
        validateSnap(snap);
        m_RotationSnap = snap;
    }

    public void setScaleSnap(float snap) {
        validateSnap(snap);
        m_ScaleSnap = snap;
    }

    public TransformTool getTransformTool() {
        return m_TransformTool;
    }

    public TransformSpace getTransformSpace() {
        return m_TransformSpace;
    }

    public boolean isSnapEnabled() {
        return m_SnapEnabled;
    }

    public float getTranslationSnap() {
        return m_TranslationSnap;
    }

    public float getRotationSnap() {
        return m_RotationSnap;
    }

    public float getScaleSnap() {
        return m_ScaleSnap;
    }

    private static void validateSnap(float snap) {
        if (!Float.isFinite(snap) || snap <= 0.0f)
            throw new IllegalArgumentException("Transform snap must be greater than zero!");
    }

}