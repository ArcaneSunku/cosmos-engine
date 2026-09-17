package atomixsoft.dev.cosmos.utils;

public record WindowConfig(String title, int width, int height) {

    public WindowConfig {
        if(title == null || title.isBlank())
            throw new IllegalArgumentException("Window title cannot be empty!");

        if(width <= 0)
            throw new IllegalArgumentException("Window Width must be greater than 0!");

        if(height <= 0)
            throw new IllegalArgumentException("Window Height must be greater than 0!");
    }

}
