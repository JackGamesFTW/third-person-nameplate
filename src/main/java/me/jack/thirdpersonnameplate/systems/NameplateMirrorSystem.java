package me.jack.thirdpersonnameplate.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.components.NameplateOwnerComponent;
import me.jack.thirdpersonnameplate.ThirdPersonNameplatePlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Watches for Nameplate changes on player entities (e.g. from other mods
 * updating DisplayNameComponent). When a player's nameplate text changes, it
 * mirrors the new text to their hologram entity and clears the player's
 * nameplate so it doesn't render as a duplicate.
 */
public class NameplateMirrorSystem extends RefChangeSystem<EntityStore, Nameplate> {
    @Nonnull
    private final ComponentType<EntityStore, Nameplate> nameplateComponentType;
    @Nonnull
    private final ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType;
    @Nonnull
    private final Query<EntityStore> query;
    @Nonnull
    private final ThirdPersonNameplatePlugin plugin;

    public NameplateMirrorSystem(
        @Nonnull ThirdPersonNameplatePlugin plugin,
        @Nonnull ComponentType<EntityStore, Nameplate> nameplateComponentType,
        @Nonnull ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType
    ) {
        this.plugin = plugin;
        this.nameplateComponentType = nameplateComponentType;
        this.ownerComponentType = ownerComponentType;
        this.query = nameplateComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, Nameplate> componentType() {
        return this.nameplateComponentType;
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Nameplate component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        mirrorAndClear(ref, component, store, commandBuffer);
    }

    @Override
    public void onComponentSet(
        @Nonnull Ref<EntityStore> ref,
        @Nullable Nameplate oldComponent,
        @Nonnull Nameplate newComponent,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        mirrorAndClear(ref, newComponent, store, commandBuffer);
    }

    @Override
    public void onComponentRemoved(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Nameplate component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (store.getComponent(ref, this.ownerComponentType) != null) {
            return;
        }

        Ref<EntityStore> hologramRef = plugin.getHologramRef(ref, store);

        if (hologramRef != null && hologramRef.isValid()) {
            Nameplate hologramNameplate = store.getComponent(hologramRef, this.nameplateComponentType);

            if (hologramNameplate != null) {
                hologramNameplate.setText("");
            }
        }
    }

    private void mirrorAndClear(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Nameplate playerNameplate,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (store.getComponent(playerRef, this.ownerComponentType) != null) {
            return;
        }

        String text = playerNameplate.getText();

        if (text.isEmpty()) {
            return;
        }

        Ref<EntityStore> hologramRef = plugin.getHologramRef(playerRef, store);

        if (hologramRef != null && hologramRef.isValid()) {
            Nameplate hologramNameplate = store.getComponent(hologramRef, this.nameplateComponentType);

            if (hologramNameplate != null) {
                hologramNameplate.setText(text);
            }

            playerNameplate.setText("");
        }
    }
}
