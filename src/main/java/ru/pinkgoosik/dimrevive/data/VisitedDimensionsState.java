package ru.pinkgoosik.dimrevive.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;

public class VisitedDimensionsState extends PersistentState {
	public final ArrayList<String> visitedDimensions = new ArrayList<>();

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt("size", this.visitedDimensions.size());
		for(int i = 0; i < this.visitedDimensions.size(); i++) {
			String dimension = this.visitedDimensions.get(i);
			nbt.putString(Integer.toString(i), dimension);
		}

		return nbt;
	}

	public static VisitedDimensionsState fromNbt(NbtCompound nbt) {
		VisitedDimensionsState manager = new VisitedDimensionsState();

		int size = nbt.getInt("size");
		for(int i = 0; i < size; i++) {
			String dimension = nbt.getString(String.valueOf(i));
			manager.visitedDimensions.add(dimension);
		}

		return manager;
	}


	public boolean wasVisited(RegistryKey<World> key) {
		this.setDirty(true);
		return visitedDimensions.contains(key.getValue().toString());
	}

	public void addDimension(RegistryKey<World> key) {
		this.visitedDimensions.add(key.getValue().toString());
	}
}
