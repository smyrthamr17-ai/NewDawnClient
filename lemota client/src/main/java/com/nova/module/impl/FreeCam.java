package com.nova.module.impl;

import com.nova.module.Module;
import com.nova.module.NumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Detaches the camera from your player so you can fly around and look. Your player stays
 * where it is and does not move while this is on (see KeyboardInputMixin / CameraMixin).
 */
public class FreeCam extends Module {
	public static boolean active;
	public static double x;
	public static double y;
	public static double z;
	public static float yaw;
	public static float pitch;

	private static long lastNanos;
	private static FreeCam instance;

	private final NumSetting speed = add(new NumSetting("Speed", 10, 2, 40, 1));

	public FreeCam() {
		super("Free Cam", "Fly the camera around while your player stays still.", Category.RENDER);
		instance = this;
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
		Vec3d eye = mc.player.getEyePos();
		x = eye.x;
		y = eye.y;
		z = eye.z;
		yaw = mc.player.getYaw();
		pitch = mc.player.getPitch();
		lastNanos = System.nanoTime();
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
	}

	@Override
	public void onTick() {
		// leaving the world while on would otherwise leave the camera detached next time
		MinecraftClient mc = mc();
		if (mc.player == null || mc.world == null) {
			setEnabled(false);
		}
	}

	public static void rotate(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
	}

	/** Moves the camera. Called once per rendered frame so the motion is smooth. */
	public static void step() {
		MinecraftClient mc = MinecraftClient.getInstance();
		long now = System.nanoTime();
		double dt = Math.min(0.1, (now - lastNanos) / 1.0e9);
		lastNanos = now;
		if (mc.currentScreen != null || mc.options == null || instance == null) {
			return;
		}

		double forward = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
		double strafe = (mc.options.rightKey.isPressed() ? 1 : 0) - (mc.options.leftKey.isPressed() ? 1 : 0);
		double vertical = (mc.options.jumpKey.isPressed() ? 1 : 0) - (mc.options.sneakKey.isPressed() ? 1 : 0);

		double yawRad = Math.toRadians(yaw);
		double pitchRad = Math.toRadians(pitch);
		double cosPitch = Math.cos(pitchRad);
		double fx = -Math.sin(yawRad) * cosPitch;
		double fy = -Math.sin(pitchRad);
		double fz = Math.cos(yawRad) * cosPitch;
		double rx = -Math.cos(yawRad);
		double rz = -Math.sin(yawRad);

		double dx = fx * forward + rx * strafe;
		double dy = fy * forward + vertical;
		double dz = fz * forward + rz * strafe;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0e-6) {
			return;
		}
		double move = instance.speed.get() * dt / len;
		x += dx * move;
		y += dy * move;
		z += dz * move;
	}
}
