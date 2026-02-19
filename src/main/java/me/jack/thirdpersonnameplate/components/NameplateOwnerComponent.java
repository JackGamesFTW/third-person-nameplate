package me.jack.thirdpersonnameplate.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NameplateOwnerComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, NameplateOwnerComponent> type;

    @Nullable
    private Ref<EntityStore> ownerRef;

    public NameplateOwnerComponent() {}

    public NameplateOwnerComponent(@Nonnull Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    public NameplateOwnerComponent(@Nonnull NameplateOwnerComponent other) {
        this.ownerRef = other.ownerRef;
    }

    @Nullable
    public Ref<EntityStore> getOwnerRef() {
        return this.ownerRef;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new NameplateOwnerComponent(this);
    }

    public static ComponentType<EntityStore, NameplateOwnerComponent> getComponentType() {
        return type;
    }

    public static void setComponentType(ComponentType<EntityStore, NameplateOwnerComponent> type) {
        NameplateOwnerComponent.type = type;
    }
}
