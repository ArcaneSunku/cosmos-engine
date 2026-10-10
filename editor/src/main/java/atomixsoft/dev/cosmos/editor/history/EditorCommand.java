package atomixsoft.dev.cosmos.editor.history;

public interface EditorCommand {

    void execute();
    void undo();

    String getName();

    default boolean isNoOp() {
        return false;
    }

}
