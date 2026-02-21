package me.jack.thirdpersonnameplate.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.components.NameplateOwnerComponent;

import javax.annotation.Nonnull;

/**
 * Syncs the hologram entity with its owning player each tick:
 * - Mirrors the player's position into the hologram's TransformComponent to keep
 *   it in the spatial index (fixes disappearing when walking away from the chunk).
 *   Also updates sentTransform so no TransformUpdate packets are generated.
 * - Detects when another mod mutates the player's nameplate text
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

        Ref<EntityStore> hologramRef = archetypeChunk.getReferenceTo(index);

        syncPosition(playerRef, hologramRef, store);
        syncNameplate(playerRef, hologramRef, store);
    }

    private void syncPosition(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Ref<EntityStore> hologramRef,
        @Nonnull Store<EntityStore> store
    ) {
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        TransformComponent hologramTransform = store.getComponent(hologramRef, TransformComponent.getComponentType());

        if (playerTransform == null || hologramTransform == null) return;

        // Update live position so the spatial index keeps the hologram near the player
        hologramTransform.setPosition(playerTransform.getPosition());

        // Mirror into sentTransform so EntityTrackerUpdate sees no delta and sends no packet
        PositionUtil.assign(hologramTransform.getSentTransform().position, hologramTransform.getPosition());
    }

    private void syncNameplate(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Ref<EntityStore> hologramRef,
        @Nonnull Store<EntityStore> store
    ) {
        Nameplate playerNameplate = store.getComponent(playerRef, this.nameplateComponentType);

        if (playerNameplate == null) return;

        String playerText = playerNameplate.getText();

        if (playerText.isEmpty()) return;

        Nameplate hologramNameplate = store.getComponent(hologramRef, this.nameplateComponentType);

        if (hologramNameplate != null) {
            hologramNameplate.setText(playerText);
        }

        playerNameplate.setText("");
    }
}
