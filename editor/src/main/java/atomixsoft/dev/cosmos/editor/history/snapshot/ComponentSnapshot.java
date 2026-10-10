package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;

import atomixsoft.dev.cosmos.scene.component.CameraComponent;
import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;

public sealed interface ComponentSnapshot permits CameraComponentSnapshot, MeshRenderComponentSnapshot {

    static ComponentSnapshot capture(Component component, AssetManager assets) {
        if (component == null)
            throw new IllegalArgumentException("Component cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        if (component instanceof CameraComponent cameraComponent)
            return CameraComponentSnapshot.capture(cameraComponent);

        if (component instanceof MeshRenderComponent meshRenderer)
            return MeshRenderComponentSnapshot.capture(meshRenderer, assets);

        throw new IllegalStateException("No Editor snapshot implementation exists for Component: " + component.getClass().getName());
    }

    Class<? extends Component> getComponentType();

    void restore(Entity entity, AssetManager assets);

}