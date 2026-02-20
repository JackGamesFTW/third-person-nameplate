package me.jack.thirdpersonnameplate;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.commands.ShowNameplateCommand;
import me.jack.thirdpersonnameplate.components.NameplateOwnerComponent;
import me.jack.thirdpersonnameplate.systems.CrouchOffsetSystem;
import me.jack.thirdpersonnameplate.systems.NameplateMirrorSystem;
import me.jack.thirdpersonnameplate.systems.NameplateSyncTickSystem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ThirdPersonNameplatePlugin extends JavaPlugin {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private final Map<UUID, Ref<EntityStore>> nameplateEntities = new ConcurrentHashMap<>();
  private final Map<Ref<EntityStore>, Ref<EntityStore>> playerToHologram = new ConcurrentHashMap<>();

  public ThirdPersonNameplatePlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Nullable
  public Ref<EntityStore> getHologramRef(
    @Nonnull Ref<EntityStore> playerRef,
    @Nonnull Store<EntityStore> store
  ) {
    return playerToHologram.get(playerRef);
  }

  public boolean hasHologram(@Nonnull UUID playerUuid) {
    Ref<EntityStore> ref = nameplateEntities.get(playerUuid);
    return ref != null && ref.isValid();
  }

  public void spawnHologram(
    @Nonnull UUID playerUuid,
    @Nonnull Ref<EntityStore> playerEntityRef,
    @Nonnull Store<EntityStore> store,
    @Nonnull PlayerRef playerRef,
    @Nonnull World world
  ) {
    Nameplate existingNameplate = store.getComponent(
      playerEntityRef,
      Nameplate.getComponentType()
    );

    String nameplateText =
      existingNameplate != null && !existingNameplate.getText().isEmpty()
        ? existingNameplate.getText()
        : playerRef.getUsername();

    if (existingNameplate != null) {
      existingNameplate.setText("");
    }

    TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());

    if (playerTransform == null) {
      return;
    }

    Vector3d playerPos = playerTransform.getPosition();
    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

    holder.addComponent(
      NetworkId.getComponentType(),
      new NetworkId(store.getExternalData().takeNextNetworkId())
    );
    holder.addComponent(
      TransformComponent.getComponentType(),
      new TransformComponent(playerPos.clone(), new Vector3f(0, 0, 0))
    );
    holder.ensureComponent(UUIDComponent.getComponentType());
    holder.addComponent(
      Nameplate.getComponentType(),
      new Nameplate(nameplateText)
    );
    holder.addComponent(
      NameplateOwnerComponent.getComponentType(),
      new NameplateOwnerComponent(playerEntityRef)
    );
    holder.addComponent(
      ProjectileComponent.getComponentType(),
      new ProjectileComponent("Projectile")
    );

    // Non-serialized: never saved to disk, clean slate on server restart
    holder.addComponent(
      EntityStore.REGISTRY.getNonSerializedComponentType(),
      NonSerialized.get()
    );

    // Mount to the player so the client handles smooth positioning.
    // The Y offset places it above the player's head.
    holder.addComponent(
      MountedComponent.getComponentType(),
      new MountedComponent(
        playerEntityRef,
        new Vector3f(0, 1.755f, 0),
        MountController.Minecart
      )
    );

    Ref<EntityStore> hologramRef = store.addEntity(holder, AddReason.SPAWN);

    store.tryRemoveComponent(hologramRef, Velocity.getComponentType());
    nameplateEntities.put(playerUuid, hologramRef);
    playerToHologram.put(playerEntityRef, hologramRef);

    LOGGER.atInfo().log("Spawned nameplate hologram for " + nameplateText);
  }

  public void removeHologram(
    @Nonnull UUID playerUuid,
    @Nonnull Ref<EntityStore> playerEntityRef,
    @Nonnull World world
  ) {
    Ref<EntityStore> hologramRef = nameplateEntities.remove(playerUuid);
    playerToHologram.remove(playerEntityRef);

    if (hologramRef == null || !hologramRef.isValid()) {
      return;
    }

    Store<EntityStore> store = world.getEntityStore().getStore();

    Nameplate hologramNameplate = store.getComponent(
      hologramRef,
      Nameplate.getComponentType()
    );

    if (hologramNameplate != null) {
      Nameplate playerNameplate = store.getComponent(
        playerEntityRef,
        Nameplate.getComponentType()
      );

      if (playerNameplate != null) {
        playerNameplate.setText(hologramNameplate.getText());
      }
    }

    store.removeEntity(hologramRef, RemoveReason.REMOVE);
    LOGGER.atInfo().log("Removed nameplate hologram for " + playerUuid);
  }

  @Override
  protected void setup() {
    ComponentType<EntityStore, NameplateOwnerComponent> ownerComponentType =
      this.getEntityStoreRegistry().registerComponent(
        NameplateOwnerComponent.class,
        NameplateOwnerComponent::new
      );

    NameplateOwnerComponent.setComponentType(ownerComponentType);

    this.getEntityStoreRegistry().registerSystem(
      new NameplateMirrorSystem(
        this,
        Nameplate.getComponentType(),
        ownerComponentType
      )
    );

    this.getEntityStoreRegistry().registerSystem(new CrouchOffsetSystem(ownerComponentType));

    this.getEntityStoreRegistry().registerSystem(
      new NameplateSyncTickSystem(
        ownerComponentType,
        Nameplate.getComponentType()
      )
    );

    this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
      Holder<EntityStore> holder = event.getHolder();
      String username = event.getPlayerRef().getUsername();

      holder.putComponent(
        Nameplate.getComponentType(),
        new Nameplate(username)
      );
    });

    this.getCommandRegistry().registerCommand(new ShowNameplateCommand(this));

    this.getEventRegistry().registerGlobal(
      DrainPlayerFromWorldEvent.class,
      event -> {
        Holder<EntityStore> holder = event.getHolder();
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());

        if (playerRef == null) {
          return;
        }

        UUID playerUuid = playerRef.getUuid();
        Ref<EntityStore> hologramRef = nameplateEntities.remove(playerUuid);
        Ref<EntityStore> entityRef = playerRef.getReference();

        if (entityRef != null) {
          playerToHologram.remove(entityRef);
        }

        if (hologramRef == null || !hologramRef.isValid()) {
          return;
        }

        World world = event.getWorld();

        world.execute(() -> {
          if (hologramRef.isValid()) {
            world
              .getEntityStore()
              .getStore()
              .removeEntity(hologramRef, RemoveReason.REMOVE);
          }
        });
      }
    );

    this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
      PlayerRef playerRef = event.getPlayerRef();
      UUID playerUuid = playerRef.getUuid();
      Ref<EntityStore> hologramRef = nameplateEntities.remove(playerUuid);

      if (hologramRef == null || !hologramRef.isValid()) {
        return;
      }

      UUID worldUuid = playerRef.getWorldUuid();

      if (worldUuid != null) {
        World world = Universe.get().getWorld(worldUuid);

        if (world != null) {
          world.execute(() -> {
            if (hologramRef.isValid()) {
              world
                .getEntityStore()
                .getStore()
                .removeEntity(hologramRef, RemoveReason.REMOVE);
            }
          });
        }
      }
    });

    LOGGER.atInfo().log("ThirdPersonNameplate setup complete");
  }
}
