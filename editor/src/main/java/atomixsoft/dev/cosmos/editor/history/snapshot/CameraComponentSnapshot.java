package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.camera.Camera;
import atomixsoft.dev.cosmos.camera.OrthographicCamera;
import atomixsoft.dev.cosmos.camera.PerspectiveCamera;

import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;

public record CameraComponentSnapshot(ProjectionType projectionType, float fieldOfView, float verticalSize,
                                      float aspectRatio, float nearPlane, float farPlane) implements ComponentSnapshot {

    public CameraComponentSnapshot {
        if (projectionType == null)
            throw new IllegalArgumentException("Camera Projection Type cannot be null!");
    }

    public static CameraComponentSnapshot capture(CameraComponent component) {
        if (component == null)
            throw new IllegalArgumentException("Camera Component cannot be null!");

        final Camera camera = component.getCamera();
        if (camera instanceof PerspectiveCamera perspective)
            return new CameraComponentSnapshot(ProjectionType.PERSPECTIVE, perspective.getFOV(), 0.0f, perspective.getAspectRatio(), perspective.getNearPlane(), perspective.getFarPlane());

        if (camera instanceof OrthographicCamera orthographic)
            return new CameraComponentSnapshot(ProjectionType.ORTHOGRAPHIC, 0.0f, orthographic.getVerticalSize(), orthographic.getAspectRatio(), orthographic.getNearPlane(), orthographic.getFarPlane());

        throw new IllegalStateException("Unsupported Camera implementation: " + camera.getClass().getName());
    }

    @Override
    public Class<? extends Component> getComponentType() {

        return CameraComponent.class;
    }

    @Override
    public void restore(Entity entity, AssetManager assets) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        entity.addComponent(new CameraComponent(createCamera()));
    }

    private Camera createCamera() {
        return switch (projectionType) {
            case PERSPECTIVE -> new PerspectiveCamera(fieldOfView, aspectRatio, nearPlane, farPlane);
            case ORTHOGRAPHIC -> new OrthographicCamera(verticalSize, aspectRatio, nearPlane, farPlane);
        };
    }

    public enum ProjectionType {
        PERSPECTIVE,
        ORTHOGRAPHIC
    }

}