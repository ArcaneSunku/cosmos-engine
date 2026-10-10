package atomixsoft.dev.cosmos.editor.history.snapshot;

import atomixsoft.dev.cosmos.asset.AssetKey;
import atomixsoft.dev.cosmos.asset.AssetManager;

import atomixsoft.dev.cosmos.render.Material;
import atomixsoft.dev.cosmos.render.Mesh;

import atomixsoft.dev.cosmos.scene.Component;
import atomixsoft.dev.cosmos.scene.Entity;

import atomixsoft.dev.cosmos.scene.component.MeshRenderComponent;

public record MeshRenderComponentSnapshot(AssetKey<Mesh> mesh, AssetKey<Material> material) implements ComponentSnapshot {

    public MeshRenderComponentSnapshot {
        if (mesh == null)
            throw new IllegalArgumentException("Mesh Asset Key cannot be null!");

        if (material == null)
            throw new IllegalArgumentException("Material Asset Key cannot be null!");
    }

    public static MeshRenderComponentSnapshot capture(MeshRenderComponent component, AssetManager assets) {
        if (component == null)
            throw new IllegalArgumentException("Mesh Renderer Component cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        return new MeshRenderComponentSnapshot(assets.getKey(Mesh.class, component.getMesh()), assets.getKey(Material.class, component.getMaterial()));
    }

    @Override
    public Class<? extends Component> getComponentType() {
        return MeshRenderComponent.class;
    }

    @Override
    public void restore(Entity entity, AssetManager assets) {
        if (entity == null)
            throw new IllegalArgumentException("Entity cannot be null!");

        if (assets == null)
            throw new IllegalArgumentException("Asset Manager cannot be null!");

        entity.addComponent(new MeshRenderComponent(assets.load(mesh), assets.load(material)));
    }

}