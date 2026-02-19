package me.jack.thirdpersonnameplate.systems;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.components.NameplateOwnerComponent;

import java.lang.reflect.Field;
import java.util.Set;
import javax.annotation.Nonnull;

public class CrouchOffsetSystem extends EntityTickingSystem<EntityStore> {

  private static final float NORMAL_Y_OFFSET = 1.755f;
  private static final float CROUCH_Y_OFFSET = 1.3f;

  private static final Field IS_NETWORK_OUTDATED_FIELD;

  static {
    try {
      IS_NETWORK_OUTDATED_FIELD = MountedComponent.class.getDeclaredField("isNetworkOutdated");
      IS_NETWORK_OUTDATED_FIELD.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to access MountedComponent.isNetworkOutdated", e);
    }
  }

  @Nonnull
  private final ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType;
  @Nonnull
  private final Set<Dependency<EntityStore>> deps;

  public CrouchOffsetSystem(
    @Nonnull ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType
  ) {
    this.ownerComponentType = ownerComponentType;
    this.deps = Set.of(
      new SystemDependency<>(Order.BEFORE, MovementStatesSystems.TickingSystem.class)
    );
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

    MovementStatesComponent msc = store.getComponent(playerRef, MovementStatesComponent.getComponentType());

    if (msc == null) return;

    MovementStates current = msc.getMovementStates();
    MovementStates sent = msc.getSentMovementStates();

    boolean wasCrouching = sent.crouching || sent.forcedCrouching;
    boolean isCrouching = current.crouching || current.forcedCrouching;

    if (wasCrouching == isCrouching) return;

    Ref<EntityStore> hologramRef = archetypeChunk.getReferenceTo(index);
    MountedComponent mounted = store.getComponent(hologramRef, MountedComponent.getComponentType());

    if (mounted == null) return;

    mounted.getAttachmentOffset().y = isCrouching ? CROUCH_Y_OFFSET : NORMAL_Y_OFFSET;

    try {
      IS_NETWORK_OUTDATED_FIELD.set(mounted, true);
    } catch (IllegalAccessException e) {}
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return this.ownerComponentType;
  }

  @Nonnull
  @Override
  public Set<Dependency<EntityStore>> getDependencies() {
    return this.deps;
  }
}
