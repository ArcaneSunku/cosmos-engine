package atomixsoft.dev.cosmos.editor.selection;

public record ScenePickRequest(int x, int y) {

    public ScenePickRequest {
        if (x < 0 || y < 0)
            throw new IllegalArgumentException("Scene Pick coordinates cannot be negative!");
    }

}