package ru.pinkgoosik.dimrevive;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pinkgoosik.dimrevive.extension.WorldExtension;

public class DimensionalRevive implements ModInitializer {
	public static final String MOD_ID = "dimrevive";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			var visitedDimensions = ((WorldExtension)player.getServer().getOverworld()).getVisitedDimensions().visitedDimensions;

			if(!destination.getRegistryKey().equals(World.OVERWORLD) && !visitedDimensions.contains(destination.getRegistryKey().getValue().toString())) {


				player.getServer().getPlayerManager().getPlayerList().forEach(serverPlayerEntity -> {
					if(serverPlayerEntity.isSpectator()) {
						FabricDimensions.teleport(serverPlayerEntity, destination, new TeleportTarget(player.getPos(), new Vec3d(0, 0, 0), player.getYaw(), player.getPitch()));
						serverPlayerEntity.changeGameMode(GameMode.SURVIVAL);

						serverPlayerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 5, 5, false, true));
						serverPlayerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 5, 5, false, true));
						serverPlayerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 5, 255, false, true));
					}
					serverPlayerEntity.sendMessage(Text.literal("New dimension was discovered: " + destination.getRegistryKey().getValue().toString() + ". All players are revived."));
				});

				visitedDimensions.add(destination.getRegistryKey().getValue().toString());
				((WorldExtension)player.getServer().getOverworld()).getVisitedDimensions().setDirty(true);
			}
		});
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
