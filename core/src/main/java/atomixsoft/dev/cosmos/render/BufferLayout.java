package atomixsoft.dev.cosmos.render;

import java.util.ArrayList;
import java.util.List;

public class BufferLayout {

    private final List<Integer> m_ComponentCounts;
    private final List<Integer> m_Offsets;

    private int m_Stride;

    public BufferLayout() {
        m_ComponentCounts = new ArrayList<>();
        m_Offsets = new ArrayList<>();

        m_Stride = 0;
    }

    public BufferLayout addFloat(int componentCount) {
        if(componentCount < 1 || componentCount > 4)
            throw new IllegalArgumentException("Float Attrib component count must be between 1 and 4!");

        m_ComponentCounts.add(componentCount);
        m_Offsets.add(m_Stride);

        m_Stride += componentCount * Float.BYTES;
        return this;
    }

    public boolean isEmpty() {
        return m_ComponentCounts.isEmpty();
    }

    public int getAttribCount() {
        return m_ComponentCounts.size();
    }

    public int getComponentCount(int index) {
        return m_ComponentCounts.get(index);
    }

    public int getOffset(int index) {
        return m_Offsets.get(index);
    }

    public int getStride() {
        return m_Stride;
    }

}
