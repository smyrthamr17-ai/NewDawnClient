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
