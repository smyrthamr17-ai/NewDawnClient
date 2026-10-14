package com.nova.util;

import com.nova.Config;
import net.minecraft.client.MinecraftClient;

/**
 * The ESP draws boxes using the FOV from the video settings. Minecraft changes the real FOV while
 * sprinting or under water ("FOV Effects"), which would shift the boxes. While an ESP is on, this
 * sets "FOV Effects" to 0 and puts the old value back afterwards.
 */
public final class FovFix {
	private static boolean active;

	private FovFix() {
	}

	public static void update(MinecraftClient mc, boolean need) {
		if (mc.options == null) {
			return;
		}
		var option = mc.options.getFovEffectScale();
		if (need) {
			if (!active) {
				active = true;
				if (Config.fovBackup == null) {
					Config.fovBackup = option.getValue();
					Config.save();
				}
			}
			if (option.getValue() > 0.0001) {
				option.setValue(0.0);
			}
		} else if (active) {
			restore(mc);
		}
	}

	public static void restore(MinecraftClient mc) {
		if (mc.options == null) {
			return;
		}
		if (Config.fovBackup != null) {
			mc.options.getFovEffectScale().setValue(Config.fovBackup);
			Config.fovBackup = null;
			Config.save();
		}
		active = false;
	}

	/** If the game was closed or crashed while the fix was active, put the old value back. */
	public static void restoreLeftover(MinecraftClient mc) {
		if (!active) {
			restore(mc);
		}
	}
}
