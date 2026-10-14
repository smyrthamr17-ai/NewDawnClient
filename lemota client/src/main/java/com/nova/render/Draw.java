package com.nova.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Small drawing helpers. All colors are ARGB (0xAARRGGBB). */
public final class Draw {
	private Draw() {
	}

	public static int clamp(double v) {
		return (int) Math.max(-8000, Math.min(8000, Math.round(v)));
	}

	public static void outline(DrawContext c, int x1, int y1, int x2, int y2, int color) {
		c.fill(x1, y1, x2, y1 + 1, color);
		c.fill(x1, y2 - 1, x2, y2, color);
		c.fill(x1, y1, x1 + 1, y2, color);
		c.fill(x2 - 1, y1, x2, y2, color);
	}

	private static final double[] LP = new double[4];
	private static final double[] LQ = new double[4];

	/**
	 * Draws a 1px line between two screen points. The line is clipped to the screen first and pixels
	 * that sit next to each other are merged into one fill, so it costs far fewer draw calls.
	 */
	public static void line(DrawContext c, double x1, double y1, double x2, double y2, int color) {
		int w = com.nova.render.Projection.screenW;
		int h = com.nova.render.Projection.screenH;
		double dx = x2 - x1;
		double dy = y2 - y1;
		LP[0] = -dx;
		LP[1] = dx;
		LP[2] = -dy;
		LP[3] = dy;
		LQ[0] = x1;
		LQ[1] = w - x1;
		LQ[2] = y1;
		LQ[3] = h - y1;
		double t0 = 0;
		double t1 = 1;
		for (int i = 0; i < 4; i++) {
			if (LP[i] == 0) {
				if (LQ[i] < 0) {
					return;
				}
			} else {
				double r = LQ[i] / LP[i];
				if (LP[i] < 0) {
					if (r > t1) {
						return;
					}
					if (r > t0) {
						t0 = r;
					}
				} else {
					if (r < t0) {
						return;
					}
					if (r < t1) {
						t1 = r;
					}
				}
			}
		}
		int ax = (int) Math.round(x1 + dx * t0);
		int ay = (int) Math.round(y1 + dy * t0);
		int bx = (int) Math.round(x1 + dx * t1);
		int by = (int) Math.round(y1 + dy * t1);
		int adx = Math.abs(bx - ax);
		int ady = Math.abs(by - ay);

		if (adx >= ady) {
			if (bx < ax) {
				int tx = ax; ax = bx; bx = tx;
				int ty = ay; ay = by; by = ty;
			}
			int runStart = ax;
			int runY = ay;
			for (int x = ax + 1; x <= bx; x++) {
				int y = ay + (int) Math.round((double) (by - ay) * (x - ax) / adx);
				if (y != runY) {
					c.fill(runStart, runY, x, runY + 1, color);
					runStart = x;
					runY = y;
				}
			}
			c.fill(runStart, runY, bx + 1, runY + 1, color);
		} else {
			if (by < ay) {
				int tx = ax; ax = bx; bx = tx;
				int ty = ay; ay = by; by = ty;
			}
			int runStart = ay;
			int runX = ax;
			for (int y = ay + 1; y <= by; y++) {
				int x = ax + (int) Math.round((double) (bx - ax) * (y - ay) / ady);
				if (x != runX) {
					c.fill(runX, runStart, runX + 1, y, color);
					runStart = y;
					runX = x;
				}
			}
			c.fill(runX, runStart, runX + 1, by + 1, color);
		}
	}

	public static int withAlpha(int color, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
	}

	public static void text(DrawContext c, String s, int x, int y, int color) {
		c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, s, x, y, color);
	}

	public static void centeredText(DrawContext c, String s, int centerX, int y, int color) {
		text(c, s, centerX - width(s) / 2, y, color);
	}

	public static int width(String s) {
		return MinecraftClient.getInstance().textRenderer.getWidth(s);
	}

	public static int lerpColor(int a, int b, float t) {
		float k = Math.max(0f, Math.min(1f, t));
		int aa = (a >>> 24) & 0xFF;
		int ar = (a >>> 16) & 0xFF;
		int ag = (a >>> 8) & 0xFF;
		int ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF;
		int br = (b >>> 16) & 0xFF;
		int bg = (b >>> 8) & 0xFF;
		int bb = b & 0xFF;
		int ra = Math.round(aa + (ba - aa) * k);
		int rr = Math.round(ar + (br - ar) * k);
		int rg = Math.round(ag + (bg - ag) * k);
		int rb = Math.round(ab + (bb - ab) * k);
		return (ra << 24) | (rr << 16) | (rg << 8) | rb;
	}
}
