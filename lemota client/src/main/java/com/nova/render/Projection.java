package com.nova.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Converts world coordinates to GUI (screen) coordinates using the real camera,
 * so ESP boxes can be drawn with the normal GUI drawing methods.
 */
public final class Projection {
	public static boolean valid;
	public static int screenW;
	public static int screenH;

	private static double cx;
	private static double cy;
	private static double cz;
	private static double fx;
	private static double fy;
	private static double fz;
	private static double rx;
	private static double rz;
	private static double ux;
	private static double uy;
	private static double uz;
	private static double tanHalf;
	private static double aspect;

	private static final double[] TMP = new double[2];

	private Projection() {
	}

	/** Reads the camera state for this frame. Call once per frame before projecting. */
	public static boolean update(MinecraftClient mc) {
		valid = false;
		if (mc.gameRenderer == null || mc.options == null) {
			return false;
		}
		Vec3d pos = mc.gameRenderer.getCamera().getCameraPos();
		cx = pos.x;
		cy = pos.y;
		cz = pos.z;

		double yaw = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
		double pitch = Math.toRadians(mc.gameRenderer.getCamera().getPitch());
		double cosPitch = Math.cos(pitch);

		// forward vector (same formula Minecraft uses for the look direction)
		fx = -Math.sin(yaw) * cosPitch;
		fy = -Math.sin(pitch);
		fz = Math.cos(yaw) * cosPitch;

		// right vector (always horizontal)
		rx = -Math.cos(yaw);
		rz = -Math.sin(yaw);

		// up vector = right x forward
		ux = -rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy;

		int fov = mc.options.getFov().getValue();
		tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
		int fbw = mc.getWindow().getFramebufferWidth();
		int fbh = mc.getWindow().getFramebufferHeight();
		aspect = (double) fbw / (double) Math.max(1, fbh);
		screenW = mc.getWindow().getScaledWidth();
		screenH = mc.getWindow().getScaledHeight();

		valid = fbw > 0 && fbh > 0;
		return valid;
	}

	public static double distanceTo(double x, double y, double z) {
		double dx = x - cx;
		double dy = y - cy;
		double dz = z - cz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Projects one point. out[0] = x, out[1] = y. Returns false when the point is behind the camera. */
	public static boolean project(double x, double y, double z, double[] out) {
		double dx = x - cx;
		double dy = y - cy;
		double dz = z - cz;
		double depth = dx * fx + dy * fy + dz * fz;
		if (depth < 0.05) {
			return false;
		}
		double xc = dx * rx + dz * rz;
		double yc = dx * ux + dy * uy + dz * uz;
		double ndcX = xc / (depth * tanHalf * aspect);
		double ndcY = yc / (depth * tanHalf);
		out[0] = (ndcX * 0.5 + 0.5) * screenW;
		out[1] = (0.5 - ndcY * 0.5) * screenH;
		return true;
	}

	/**
	 * Projects the 8 corners of a box and returns the 2D rectangle around them in out = {minX, minY, maxX, maxY}.
	 * Returns false if a corner is behind the camera or the rectangle is completely off screen.
	 */
	public static boolean projectBox(double x1, double y1, double z1, double x2, double y2, double z2, double[] out) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double x = (i & 1) == 0 ? x1 : x2;
			double y = (i & 2) == 0 ? y1 : y2;
			double z = (i & 4) == 0 ? z1 : z2;
			if (!project(x, y, z, TMP)) {
				return false;
			}
			minX = Math.min(minX, TMP[0]);
			minY = Math.min(minY, TMP[1]);
			maxX = Math.max(maxX, TMP[0]);
			maxY = Math.max(maxY, TMP[1]);
		}
		if (maxX < 0 || maxY < 0 || minX > screenW || minY > screenH) {
			return false;
		}
		out[0] = minX;
		out[1] = minY;
		out[2] = maxX;
		out[3] = maxY;
		return true;
	}
}
