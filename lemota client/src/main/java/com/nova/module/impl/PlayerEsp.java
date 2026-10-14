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
import net.minecraft.util.math.Box;

public class PlayerEsp extends Module {
	private static final int NORMAL_COLOR = 0xFF55FFFF;
	private static final int HIDDEN_COLOR = 0xFFFF55FF;

	private static final double[] BOX = new double[4];
	private static final double[] POS = new double[3];
	private static final double[][] PTS = new double[8][2];

	private final NumSetting range = add(new NumSetting("Range", 128, 16, 512, 16));
	private final BoolSetting names = add(new BoolSetting("Names", true));
	private final BoolSetting health = add(new BoolSetting("Health", true));
	private final BoolSetting distance = add(new BoolSetting("Distance", true));
	private final BoolSetting showHidden = add(new BoolSetting("Invisible/Spectators", true));
	private final BoolSetting box3d = add(new BoolSetting("3D Hitbox", true));
	private final BoolSetting tracers = add(new BoolSetting("Tracers", true));
	private final BoolSetting fade = add(new BoolSetting("Fade With Distance", true));
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
		double[] box = BOX;
		double[] pos = POS;

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player) {
				continue;
			}
			boolean hidden = player.isInvisible() || player.isSpectator();
			if (hidden && !showHidden.get()) {
				continue;
			}

			Tracker.lerp(player, tickProgress, pos);
			double dist = Projection.distanceTo(pos[0], pos[1], pos[2]);
			if (dist > maxRange) {
				continue;
			}

			// the real hitbox of the player, moved to the smoothed position
			Box bb = player.getBoundingBox();
			double ox = pos[0] - player.getX();
			double oy = pos[1] - player.getY();
			double oz = pos[2] - player.getZ();
			double bx1 = bb.minX + ox;
			double by1 = bb.minY + oy;
			double bz1 = bb.minZ + oz;
			double bx2 = bb.maxX + ox;
			double by2 = bb.maxY + oy;
			double bz2 = bb.maxZ + oz;
			if (!Projection.projectBox(bx1, by1, bz1, bx2, by2, bz2, box)) {
				continue;
			}

			int x1 = Draw.clamp(box[0]);
			int y1 = Draw.clamp(box[1]);
			int x2 = Draw.clamp(box[2]);
			int y2 = Draw.clamp(box[3]);

			// farther away = dimmer
			int alpha = 255;
			if (fade.get()) {
				double ratio = Math.min(1.0, dist / maxRange);
				alpha = (int) Math.round(255 - ratio * 165);
			}
			int color = Draw.withAlpha(hidden ? HIDDEN_COLOR : NORMAL_COLOR, alpha);
			int textColor = Draw.withAlpha(0xFFFFFFFF, alpha);

			if (box3d.get()) {
				drawBox3d(context, bx1, by1, bz1, bx2, by2, bz2, color);
			} else {
				Draw.outline(context, x1 - 1, y1 - 1, x2 + 1, y2 + 1, Draw.withAlpha(0xFF000000, alpha / 2));
				Draw.outline(context, x1, y1, x2, y2, color);
			}

			if (tracers.get()) {
				Draw.line(context, Projection.screenW / 2.0, Projection.screenH, (x1 + x2) / 2.0, y2, color);
			}

			float hp = player.getHealth();
			float maxHp = Math.max(1f, player.getMaxHealth());

			if (health.get()) {
				float ratio = Math.max(0f, Math.min(1f, hp / maxHp));
				int barBottom = y2;
				int barTop = y2 - Math.max(1, Math.round((y2 - y1) * ratio));
				context.fill(x1 - 5, y1, x1 - 3, y2, Draw.withAlpha(0xFF000000, alpha / 2));
				context.fill(x1 - 5, barTop, x1 - 3, barBottom, Draw.withAlpha(Draw.lerpColor(0xFFFF4444, 0xFF44FF44, ratio), alpha));
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
					info.append(Math.round(hp)).append(" HP");
				}
				if (distance.get()) {
					if (info.length() > 0) {
						info.append("  ");
					}
					info.append(Math.round(dist)).append("m");
				}
				Draw.centeredText(context, info.toString(), (x1 + x2) / 2, y2 + 3, textColor);
			}
		}
	}

	private static final double[] CORNER = new double[2];

	/** Draws the 12 edges of a world-space box as lines. Skipped if any corner is behind the camera. */
	private static void drawBox3d(DrawContext context, double x1, double y1, double z1,
			double x2, double y2, double z2, int color) {
		double[][] pts = PTS;
		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 0 ? x1 : x2;
			double y = (i & 2) == 0 ? y1 : y2;
			double z = (i & 4) == 0 ? z1 : z2;
			if (!Projection.project(x, y, z, CORNER)) {
				return;
			}
			pts[i][0] = CORNER[0];
			pts[i][1] = CORNER[1];
		}
		for (int i = 0; i < 8; i++) {
			for (int bit = 1; bit <= 4; bit <<= 1) {
				if ((i & bit) == 0) {
					int j = i | bit;
					Draw.line(context, pts[i][0], pts[i][1], pts[j][0], pts[j][1], color);
				}
			}
		}
	}
}
