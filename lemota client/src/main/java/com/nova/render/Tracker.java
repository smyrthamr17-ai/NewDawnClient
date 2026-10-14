package com.nova.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Remembers where every player was at the end of the previous and the current client tick,
 * so the ESP can interpolate positions smoothly between ticks.
 */
public final class Tracker {
	private static final Map<Integer, double[]> POSITIONS = new HashMap<>();
	private static final Set<Integer> SEEN = new HashSet<>();

	private Tracker() {
	}

	public static void clear() {
		POSITIONS.clear();
	}

	public static void tick(MinecraftClient mc) {
		if (mc.world == null) {
			POSITIONS.clear();
			return;
		}
		SEEN.clear();
		for (PlayerEntity player : mc.world.getPlayers()) {
			int id = player.getId();
			SEEN.add(id);
			double x = player.getX();
			double y = player.getY();
			double z = player.getZ();
			double[] a = POSITIONS.get(id);
			if (a == null) {
				POSITIONS.put(id, new double[] {x, y, z, x, y, z});
			} else {
				a[0] = a[3];
				a[1] = a[4];
				a[2] = a[5];
				a[3] = x;
				a[4] = y;
				a[5] = z;
			}
		}
		POSITIONS.keySet().retainAll(SEEN);
	}

	/** Same as lerp(player, tickProgress) but writes into out (length 3) instead of allocating. */
	public static void lerp(PlayerEntity player, float tickProgress, double[] out) {
		double[] a = POSITIONS.get(player.getId());
		if (a == null) {
			out[0] = player.getX();
			out[1] = player.getY();
			out[2] = player.getZ();
			return;
		}
		double t = tickProgress;
		out[0] = a[0] + (a[3] - a[0]) * t;
		out[1] = a[1] + (a[4] - a[1]) * t;
		out[2] = a[2] + (a[5] - a[2]) * t;
	}

	/** Returns the interpolated {x, y, z} of the player. */
	public static double[] lerp(PlayerEntity player, float tickProgress) {
		double[] a = POSITIONS.get(player.getId());
		if (a == null) {
			return new double[] {player.getX(), player.getY(), player.getZ()};
		}
		double t = tickProgress;
		return new double[] {
			a[0] + (a[3] - a[0]) * t,
			a[1] + (a[4] - a[1]) * t,
			a[2] + (a[5] - a[2]) * t
		};
	}
}
