package ru.pinkgoosik.dimrevive.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.pinkgoosik.dimrevive.extension.PlayerExtension;

import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerMixin extends PlayerEntity implements PlayerExtension {

	@Shadow
	@Final
	public MinecraftServer server;

	@Shadow
	protected abstract void forgiveMobAnger();

	@Shadow
	public abstract boolean changeGameMode(GameMode gameMode);

	public PlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Override
	public void onSilentDeath(DamageSource source) {
		this.setHealth(this.getMaxHealth());
		this.emitGameEvent(GameEvent.ENTITY_DIE);
		boolean bl = this.getWorld().getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES);
		if (bl) {
			Text text = this.getDamageTracker().getDeathMessage();
			server.getPlayerManager().broadcast(text, false);
		}

		this.dropShoulderEntities();
		if (this.getWorld().getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
			forgiveMobAnger();
		}

		if (!this.isSpectator()) {
			this.drop(source);
		}

		this.getScoreboard().forEachScore(ScoreboardCriterion.DEATH_COUNT, this.getEntityName(), ScoreboardPlayerScore::incrementScore);
		LivingEntity livingEntity = this.getPrimeAdversary();
		if (livingEntity != null) {
			this.incrementStat(Stats.KILLED_BY.getOrCreateStat(livingEntity.getType()));
			livingEntity.updateKilledAdvancementCriterion(this, this.scoreAmount, source);
			this.onKilledBy(livingEntity);
		}

		this.getWorld().sendEntityStatus(this, (byte)3);
		this.incrementStat(Stats.DEATHS);
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
		this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		this.extinguish();
		this.setFrozenTicks(0);
		this.setOnFire(false);
		this.getDamageTracker().update();
		this.setLastDeathPos(Optional.of(GlobalPos.create(this.getWorld().getRegistryKey(), this.getBlockPos())));
		this.changeGameMode(GameMode.SPECTATOR);
	}
}
