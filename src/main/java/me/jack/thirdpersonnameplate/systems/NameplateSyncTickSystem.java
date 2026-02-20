package me.jack.thirdpersonnameplate.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.components.NameplateOwnerComponent;

import javax.annotation.Nonnull;

/**
 * Detects when another mod mutates a player's
 * Nameplate text while their hologram is active.
 */
public class NameplateSyncTickSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType;
    @Nonnull
    private final ComponentType<EntityStore, Nameplate> nameplateComponentType;
    @Nonnull
    private final Query<EntityStore> query;

    public NameplateSyncTickSystem(
        @Nonnull ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType,
        @Nonnull ComponentType<EntityStore, Nameplate> nameplateComponentType
    ) {
        this.ownerComponentType = ownerComponentType;
        this.nameplateComponentType = nameplateComponentType;
        this.query = ownerComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void tick(
        float dt,
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NameplateOwnerComponent owner = archetypeChunk.getComponent(index, this.ownerComponentType);

        if (owner == null) return;

        Ref<EntityStore> playerRef = owner.getOwnerRef();

        if (playerRef == null || !playerRef.isValid()) return;

        Nameplate playerNameplate = store.getComponent(playerRef, this.nameplateComponentType);

        if (playerNameplate == null) return;

        String playerText = playerNameplate.getText();

        if (playerText.isEmpty()) return;

        Ref<EntityStore> hologramRef = archetypeChunk.getReferenceTo(index);
        Nameplate hologramNameplate = store.getComponent(hologramRef, this.nameplateComponentType);

        if (hologramNameplate != null) {
            hologramNameplate.setText(playerText);
        }

        playerNameplate.setText("");
    }
}
