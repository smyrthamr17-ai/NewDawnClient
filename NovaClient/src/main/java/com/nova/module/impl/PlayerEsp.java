package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import com.nova.module.NumSetting;
import com.nova.render.Draw;
import com.nova.render.Projection;
import com.nova.render.Tracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

public class PlayerEsp extends Module {
	private static final int NORMAL_COLOR = 0xFF55FFFF;
	private static final int HIDDEN_COLOR = 0xFFFF55FF;

	private final NumSetting range = add(new NumSetting("Range", 128, 16, 512, 16));
	private final BoolSetting names = add(new BoolSetting("Names", true));
	private final BoolSetting health = add(new BoolSetting("Health", true));
	private final BoolSetting distance = add(new BoolSetting("Distance", true));
	private final BoolSetting showHidden = add(new BoolSetting("Invisible/Spectators", true));
	private final BoolSetting fixFov = add(new BoolSetting("Fix FOV Effects", true));

	public PlayerEsp() {
		super("Player ESP", "Draws a box around players, also through walls.", Category.RENDER);
	}

	@Override
	public boolean wantsStableFov() {
		return fixFov.get();
	}

	@Override
	public void onHud(DrawContext context, float tickProgress) {
		MinecraftClient mc = mc();
		if (!Projection.valid || mc.world == null || mc.player == null) {
			return;
		}
		double maxRange = range.get();
		double[] box = new double[4];

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player) {
				continue;
			}
			boolean hidden = player.isInvisible() || player.isSpectator();
			if (hidden && !showHidden.get()) {
				continue;
			}

			double[] pos = Tracker.lerp(player, tickProgress);
			double dist = Projection.distanceTo(pos[0], pos[1], pos[2]);
			if (dist > maxRange) {
				continue;
			}

			double halfWidth = player.getWidth() / 2.0;
			double height = player.getHeight();
			if (!Projection.projectBox(pos[0] - halfWidth, pos[1], pos[2] - halfWidth,
					pos[0] + halfWidth, pos[1] + height, pos[2] + halfWidth, box)) {
				continue;
			}

			int x1 = Draw.clamp(box[0]);
			int y1 = Draw.clamp(box[1]);
			int x2 = Draw.clamp(box[2]);
			int y2 = Draw.clamp(box[3]);
			int color = hidden ? HIDDEN_COLOR : NORMAL_COLOR;

			// dark outline first, colored outline on top
			Draw.outline(context, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xAA000000);
			Draw.outline(context, x1, y1, x2, y2, color);

			float hp = player.getHealth();
			float maxHp = Math.max(1f, player.getMaxHealth());

			if (health.get()) {
				float ratio = Math.max(0f, Math.min(1f, hp / maxHp));
				int barBottom = y2;
				int barTop = y2 - Math.max(1, Math.round((y2 - y1) * ratio));
				context.fill(x1 - 5, y1, x1 - 3, y2, 0xAA000000);
				context.fill(x1 - 5, barTop, x1 - 3, barBottom, Draw.lerpColor(0xFFFF4444, 0xFF44FF44, ratio));
			}

			if (names.get()) {
				String label = player.getName().getString();
				if (hidden) {
					label = label + (player.isSpectator() ? " [Spectator]" : " [Invisible]");
				}
				Draw.centeredText(context, label, (x1 + x2) / 2, y1 - 11, color);
			}

			if (distance.get() || health.get()) {
				StringBuilder info = new StringBuilder();
				if (health.get()) {
					info.append(String.format(Locale.ROOT, "%.0f HP", hp));
				}
				if (distance.get()) {
					if (info.length() > 0) {
						info.append("  ");
					}
					info.append(String.format(Locale.ROOT, "%.0fm", dist));
				}
				Draw.centeredText(context, info.toString(), (x1 + x2) / 2, y2 + 3, 0xFFFFFFFF);
			}
		}
	}
}
