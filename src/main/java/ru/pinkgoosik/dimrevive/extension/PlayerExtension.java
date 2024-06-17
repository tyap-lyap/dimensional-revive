package ru.pinkgoosik.dimrevive.extension;

import net.minecraft.entity.damage.DamageSource;

public interface PlayerExtension {

	void onSilentDeath(DamageSource source);
}
