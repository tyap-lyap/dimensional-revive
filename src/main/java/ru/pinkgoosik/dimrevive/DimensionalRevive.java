package ru.pinkgoosik.dimrevive;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pinkgoosik.dimrevive.extension.PlayerExtension;
import ru.pinkgoosik.dimrevive.extension.WorldExtension;

public class DimensionalRevive implements ModInitializer {
	public static final String MOD_ID = "dimrevive";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
			if(entity instanceof ServerPlayerEntity player && player instanceof PlayerExtension ex && player.getServer().isHardcore() && player.interactionManager.getGameMode().equals(GameMode.SURVIVAL)) {
				SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, player.getServerWorld());
				skeleton.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());

				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				GameProfile gameProfile = player.getGameProfile();
				head.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), gameProfile));

				skeleton.equipStack(EquipmentSlot.HEAD, head);

				skeleton.equipStack(EquipmentSlot.CHEST, player.getEquippedStack(EquipmentSlot.CHEST).copy());
				skeleton.equipStack(EquipmentSlot.LEGS, player.getEquippedStack(EquipmentSlot.LEGS).copy());
				skeleton.equipStack(EquipmentSlot.FEET, player.getEquippedStack(EquipmentSlot.FEET).copy());
				skeleton.equipStack(EquipmentSlot.MAINHAND, player.getMainHandStack().copy());
				skeleton.equipStack(EquipmentSlot.OFFHAND, player.getOffHandStack().copy());

				skeleton.setHealth(0);
				player.getServerWorld().spawnEntity(skeleton);

				ex.onSilentDeath(damageSource);
				return false;
			}
			return true;
		});

		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			var visitedDimensions = ((WorldExtension)player.getServer().getOverworld()).getVisitedDimensions();

			if(player.interactionManager.getGameMode().equals(GameMode.SURVIVAL) && player.getServer().isHardcore() && !destination.getRegistryKey().equals(World.OVERWORLD) && !visitedDimensions.wasVisited(destination.getRegistryKey())) {

				player.getServer().getPlayerManager().broadcast(Text.literal("New dimension was discovered: §6" + formatDimensionName(destination.getRegistryKey()) + "§f. All players are revived."), false);
				visitedDimensions.addDimension(destination.getRegistryKey());

				player.getServer().getPlayerManager().getPlayerList().forEach(deadPlayer -> {
					if(deadPlayer.isSpectator()) {
						FabricDimensions.teleport(deadPlayer, destination, new TeleportTarget(player.getPos(), new Vec3d(0, 0, 0), player.getYaw(), player.getPitch()));
						deadPlayer.changeGameMode(GameMode.SURVIVAL);
						deadPlayer.setPortalCooldown(20 * 5);

						deadPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 10, 5, false, false));
						deadPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 10, 5, false, false));
						deadPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 255, false, false));
					}
				});
			}
		});
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	public static String formatDimensionName(RegistryKey<World> key) {
		return WordUtils.capitalize(key.getValue().getPath().replace("_", " "));
	}
}
