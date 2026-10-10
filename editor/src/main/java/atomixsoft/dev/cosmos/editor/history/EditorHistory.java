package atomixsoft.dev.cosmos.editor.history;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EditorHistory {

    private static final int MAX_COMMANDS = 256;

    private final Deque<EditorCommand> m_UndoStack;
    private final Deque<EditorCommand> m_RedoStack;

    private boolean m_ApplyingHistory;

    public EditorHistory() {
        m_UndoStack = new ArrayDeque<>();
        m_RedoStack = new ArrayDeque<>();

        m_ApplyingHistory = false;
    }

    private static void validateCommand(EditorCommand command) {
        if (command == null)
            throw new IllegalArgumentException("Editor Command cannot be null!");

        final String name = command.getName();
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Editor Command name cannot be empty!");
    }

    public void execute(EditorCommand command) {
        validateCommand(command);

        if (command.isNoOp())
            return;

        ensureNotApplying();
        command.execute();

        pushUndo(command);
        m_RedoStack.clear();
    }

    public void recordExecuted(EditorCommand command) {
        validateCommand(command);

        if (command.isNoOp())
            return;

        ensureNotApplying();
        pushUndo(command);

        m_RedoStack.clear();
    }

    public boolean undo() {
        if (m_UndoStack.isEmpty())
            return false;

        ensureNotApplying();
        final EditorCommand command = m_UndoStack.peekFirst();

        m_ApplyingHistory = true;

        try {
            command.undo();
        } finally {
            m_ApplyingHistory = false;
        }

        m_UndoStack.removeFirst();
        m_RedoStack.addFirst(command);

        return true;
    }

    public boolean redo() {
        if (m_RedoStack.isEmpty())
            return false;

        ensureNotApplying();
        final EditorCommand command = m_RedoStack.peekFirst();

        m_ApplyingHistory = true;
        try {
            command.execute();
        } finally {
            m_ApplyingHistory = false;
        }

        m_RedoStack.removeFirst();
        m_UndoStack.addFirst(command);

        trimUndoStack();

        return true;
    }

    public void clear() {
        if (m_ApplyingHistory)
            throw new IllegalStateException("Cannot clear Editor History while a command is being applied!");

        m_UndoStack.clear();
        m_RedoStack.clear();
    }

    public boolean canUndo() {
        return !m_UndoStack.isEmpty();
    }

    public boolean canRedo() {
        return !m_RedoStack.isEmpty();
    }

    public boolean isApplyingHistory() {
        return m_ApplyingHistory;
    }

    public String getUndoName() {
        final EditorCommand command = m_UndoStack.peekFirst();
        return command == null ? null : command.getName();
    }

    public String getRedoName() {
        final EditorCommand command = m_RedoStack.peekFirst();
        return command == null ? null : command.getName();
    }

    private void pushUndo(EditorCommand command) {
        m_UndoStack.addFirst(command);
        trimUndoStack();
    }

    private void trimUndoStack() {
        while (m_UndoStack.size() > MAX_COMMANDS)
            m_UndoStack.removeLast();
    }

    private void ensureNotApplying() {
        if (m_ApplyingHistory)
            throw new IllegalStateException("Editor History is already applying a command!");
    }

}