package com.nova.module.impl;

import com.nova.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Lets you look around with the camera without turning your player.
 * The mouse moves only the camera while this is on (see EntityMixin / CameraMixin).
 */
public class FreeLook extends Module {
	public static boolean active;
	public static float yaw;
	public static float pitch;

	public FreeLook() {
		super("Free Look", "Rotate the camera freely without turning your player.", Category.RENDER);
	}

	@Override
	public boolean persistsEnabled() {
		return false;
	}

	@Override
	protected void onEnable() {
		MinecraftClient mc = mc();
		if (mc.player == null) {
			setEnabled(false);
			return;
		}
		yaw = mc.player.getYaw();
		pitch = mc.player.getPitch();
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
	}

	public static void rotate(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
	}
}
