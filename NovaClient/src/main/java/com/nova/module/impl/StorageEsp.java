package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import com.nova.module.NumSetting;
import com.nova.render.Draw;
import com.nova.render.Projection;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StorageEsp extends Module {
	private static final int CHEST_COLOR = 0xFFFFA726;
	private static final int TRAPPED_COLOR = 0xFFEF5350;
	private static final int ENDER_COLOR = 0xFFAB47BC;
	private static final int BARREL_COLOR = 0xFF8D6E63;
	private static final int SHULKER_COLOR = 0xFFEC407A;
	private static final int OTHER_COLOR = 0xFF90A4AE;

	private final NumSetting range = add(new NumSetting("Range", 96, 16, 256, 16));
	private final BoolSetting chests = add(new BoolSetting("Chests", true));
	private final BoolSetting enderChests = add(new BoolSetting("Ender Chests", true));
	private final BoolSetting barrels = add(new BoolSetting("Barrels", true));
	private final BoolSetting shulkers = add(new BoolSetting("Shulker Boxes", true));
	private final BoolSetting others = add(new BoolSetting("Hoppers/Furnaces", false));
	private final BoolSetting labels = add(new BoolSetting("Labels", false));
	private final BoolSetting fixFov = add(new BoolSetting("Fix FOV Effects", true));

	private List<Target> targets = new ArrayList<>();
	private int tickCounter;

	private static final class Target {
		final int x;
		final int y;
		final int z;
		final int color;
		final boolean chestShape;
		final String label;

		Target(int x, int y, int z, int color, boolean chestShape, String label) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.color = color;
			this.chestShape = chestShape;
			this.label = label;
		}
	}

	public StorageEsp() {
		super("Storage ESP", "Shows chests, barrels, shulker boxes and more through walls.", Category.RENDER);
	}

	@Override
	public boolean wantsStableFov() {
		return fixFov.get();
	}

	@Override
	protected void onEnable() {
		tickCounter = 0;
	}

	@Override
	protected void onDisable() {
		targets = new ArrayList<>();
	}

	@Override
	public void onTick() {
		if (tickCounter++ % 10 != 0) {
			return;
		}
		scan();
	}

	private void scan() {
		MinecraftClient mc = mc();
		if (mc.world == null || mc.player == null) {
			return;
		}
		List<Target> found = new ArrayList<>();
		double px = mc.player.getX();
		double py = mc.player.getY();
		double pz = mc.player.getZ();
		double maxRange = range.get();
		double maxRangeSq = maxRange * maxRange;

		int viewDistance = mc.options.getViewDistance().getValue();
		int chunkRadius = Math.min(viewDistance, (int) Math.ceil(maxRange / 16.0) + 1);
		int centerChunkX = ((int) Math.floor(px)) >> 4;
		int centerChunkZ = ((int) Math.floor(pz)) >> 4;

		for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
			for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
				WorldChunk chunk = mc.world.getChunk(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					BlockPos pos = blockEntity.getPos();
					double dx = pos.getX() + 0.5 - px;
					double dy = pos.getY() + 0.5 - py;
					double dz = pos.getZ() + 0.5 - pz;
					if (dx * dx + dy * dy + dz * dz > maxRangeSq) {
						continue;
					}
					Target target = classify(blockEntity, pos);
					if (target != null) {
						found.add(target);
					}
				}
			}
		}
		targets = found;
	}

	private Target classify(BlockEntity be, BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if (be instanceof TrappedChestBlockEntity) {
			return chests.get() ? new Target(x, y, z, TRAPPED_COLOR, true, "Trapped Chest") : null;
		}
		if (be instanceof ChestBlockEntity) {
			return chests.get() ? new Target(x, y, z, CHEST_COLOR, true, "Chest") : null;
		}
		if (be instanceof EnderChestBlockEntity) {
			return enderChests.get() ? new Target(x, y, z, ENDER_COLOR, true, "Ender Chest") : null;
		}
		if (be instanceof BarrelBlockEntity) {
			return barrels.get() ? new Target(x, y, z, BARREL_COLOR, false, "Barrel") : null;
		}
		if (be instanceof ShulkerBoxBlockEntity) {
			return shulkers.get() ? new Target(x, y, z, SHULKER_COLOR, false, "Shulker Box") : null;
		}
		if (be instanceof HopperBlockEntity) {
			return others.get() ? new Target(x, y, z, OTHER_COLOR, false, "Hopper") : null;
		}
		if (be instanceof DispenserBlockEntity) {
			return others.get() ? new Target(x, y, z, OTHER_COLOR, false, "Dispenser/Dropper") : null;
		}
		if (be instanceof AbstractFurnaceBlockEntity) {
			return others.get() ? new Target(x, y, z, OTHER_COLOR, false, "Furnace") : null;
		}
		if (be instanceof BrewingStandBlockEntity) {
			return others.get() ? new Target(x, y, z, OTHER_COLOR, false, "Brewing Stand") : null;
		}
		return null;
	}

	@Override
	public void onHud(DrawContext context, float tickProgress) {
		if (!Projection.valid) {
			return;
		}
		double maxRange = range.get();
		double[] box = new double[4];

		for (Target t : targets) {
			double dist = Projection.distanceTo(t.x + 0.5, t.y + 0.5, t.z + 0.5);
			if (dist > maxRange) {
				continue;
			}
			double inset = t.chestShape ? 0.0625 : 0.0;
			double top = t.chestShape ? 0.875 : 1.0;
			if (!Projection.projectBox(t.x + inset, t.y, t.z + inset,
					t.x + 1 - inset, t.y + top, t.z + 1 - inset, box)) {
				continue;
			}
			int x1 = Draw.clamp(box[0]);
			int y1 = Draw.clamp(box[1]);
			int x2 = Draw.clamp(box[2]);
			int y2 = Draw.clamp(box[3]);
			Draw.outline(context, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xAA000000);
			Draw.outline(context, x1, y1, x2, y2, t.color);
			if (labels.get() && dist < 48) {
				String text = t.label + " " + String.format(Locale.ROOT, "%.0fm", dist);
				Draw.centeredText(context, text, (x1 + x2) / 2, y1 - 11, t.color);
			}
		}
	}
}
