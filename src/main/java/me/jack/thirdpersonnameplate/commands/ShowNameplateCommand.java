package me.jack.thirdpersonnameplate.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jack.thirdpersonnameplate.ThirdPersonNameplatePlugin;

import java.util.UUID;
import javax.annotation.Nonnull;

public class ShowNameplateCommand extends AbstractPlayerCommand {

  @Nonnull
  private final ThirdPersonNameplatePlugin plugin;

  public ShowNameplateCommand(@Nonnull ThirdPersonNameplatePlugin plugin) {
    super("shownameplate", "Toggles your own nameplate visibility in third person");

    this.plugin = plugin;
    this.setPermissionGroup(GameMode.Adventure);
  }

  @Override
  protected void execute(
    @Nonnull CommandContext context,
    @Nonnull Store<EntityStore> store,
    @Nonnull Ref<EntityStore> ref,
    @Nonnull PlayerRef playerRef,
    @Nonnull World world
  ) {
    UUID playerUuid = playerRef.getUuid();

    if (plugin.hasHologram(playerUuid)) {
      plugin.removeHologram(playerUuid, ref, world);
      context.sendMessage(Message.raw("Nameplate hidden."));
    } else {
      plugin.spawnHologram(playerUuid, ref, store, playerRef, world);
      context.sendMessage(Message.raw("Nameplate shown."));
    }
  }
}
