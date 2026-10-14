package com.nova.gui;

import com.nova.Config;
import com.nova.module.BoolSetting;
import com.nova.module.Module;
import com.nova.module.ModuleManager;
import com.nova.module.NumSetting;
import com.nova.module.Setting;
import com.nova.render.Draw;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Nova menu. Left click a module to turn it on or off, right click it to open its settings.
 * Drag the bars to change number settings.
 */
public class ClickGui extends Screen {
	private static final int COLUMN_WIDTH = 126;
	private static final int GAP = 10;
	private static final int HEADER_HEIGHT = 18;
	private static final int MODULE_HEIGHT = 16;
	private static final int BOOL_HEIGHT = 14;
	private static final int NUMBER_HEIGHT = 22;
	private static final int TOP = 50;

	private static final int ACCENT = 0xFF8E5CFF;
	private static final int ACCENT_DARK = 0xFF4A3480;
	private static final int ROW = 0xD0242536;
	private static final int ROW_HOVER = 0xD0313350;
	private static final int TRACK = 0xFF3A3B52;
	private static final int TEXT = 0xFFE6E6F0;
	private static final int DIM = 0xFF9A9AB0;

	private final Set<Module> expanded = new HashSet<>();
	private final List<Row> rows = new ArrayList<>();
	private Row dragging;

	private static final class Row {
		final int x;
		final int y;
		final int w;
		final int h;
		final Module module;
		final Setting setting;
		final Module.Category header;

		Row(int x, int y, int w, int h, Module module, Setting setting, Module.Category header) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.module = module;
			this.setting = setting;
			this.header = header;
		}

		boolean hit(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public ClickGui() {
		super(Text.literal("Nova"));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void layout() {
		rows.clear();
		Module.Category[] categories = Module.Category.values();
		int totalWidth = categories.length * COLUMN_WIDTH + (categories.length - 1) * GAP;
		int startX = (width - totalWidth) / 2;
		for (int c = 0; c < categories.length; c++) {
			int x = startX + c * (COLUMN_WIDTH + GAP);
			int y = TOP;
			rows.add(new Row(x, y, COLUMN_WIDTH, HEADER_HEIGHT, null, null, categories[c]));
			y += HEADER_HEIGHT + 2;
			for (Module module : ModuleManager.byCategory(categories[c])) {
				rows.add(new Row(x, y, COLUMN_WIDTH, MODULE_HEIGHT, module, null, null));
				y += MODULE_HEIGHT + 1;
				if (expanded.contains(module)) {
					for (Setting setting : module.settings) {
						int h = setting instanceof NumSetting ? NUMBER_HEIGHT : BOOL_HEIGHT;
						rows.add(new Row(x + 4, y, COLUMN_WIDTH - 4, h, module, setting, null));
						y += h + 1;
					}
					y += 3;
				}
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		layout();
		context.fill(0, 0, width, height, 0xA0000000);

		Draw.centeredText(context, "NOVA CLIENT", width / 2, 18, ACCENT);
		Draw.centeredText(context, "Left click: on/off    Right click: settings    ESC: close", width / 2, 31, DIM);

		Module hovered = null;
		for (Row row : rows) {
			boolean hover = row.hit(mouseX, mouseY);
			if (row.header != null) {
				context.fill(row.x, row.y, row.x + row.w, row.y + row.h, ACCENT_DARK);
				context.fill(row.x, row.y + row.h - 2, row.x + row.w, row.y + row.h, ACCENT);
				Draw.text(context, row.header.label, row.x + 6, row.y + 5, 0xFFFFFFFF);
			} else if (row.setting == null) {
				drawModuleRow(context, row, hover);
				if (hover) {
					hovered = row.module;
				}
			} else if (row.setting instanceof BoolSetting bool) {
				drawBoolRow(context, row, bool, hover);
			} else if (row.setting instanceof NumSetting number) {
				drawNumberRow(context, row, number, hover);
			}
		}

		if (hovered != null) {
			Draw.centeredText(context, hovered.description, width / 2, height - 22, TEXT);
		}
	}

	private void drawModuleRow(DrawContext context, Row row, boolean hover) {
		Module module = row.module;
		module.anim += ((module.isEnabled() ? 1f : 0f) - module.anim) * 0.25f;
		int base = hover ? ROW_HOVER : ROW;
		int background = Draw.lerpColor(base, ACCENT_DARK, module.anim);
		context.fill(row.x, row.y, row.x + row.w, row.y + row.h, background);
		context.fill(row.x, row.y, row.x + 2, row.y + row.h, Draw.lerpColor(TRACK, ACCENT, module.anim));
		Draw.text(context, module.name, row.x + 7, row.y + 4, module.isEnabled() ? 0xFFFFFFFF : TEXT);
		if (!module.settings.isEmpty()) {
			String arrow = expanded.contains(module) ? "v" : ">";
			Draw.text(context, arrow, row.x + row.w - 10, row.y + 4, DIM);
		}
	}

	private void drawBoolRow(DrawContext context, Row row, BoolSetting setting, boolean hover) {
		context.fill(row.x, row.y, row.x + row.w, row.y + row.h, hover ? ROW_HOVER : ROW);
		Draw.text(context, setting.name, row.x + 5, row.y + 3, TEXT);
		int boxX = row.x + row.w - 14;
		int boxY = row.y + 3;
		context.fill(boxX, boxY, boxX + 8, boxY + 8, TRACK);
		if (setting.get()) {
			context.fill(boxX + 1, boxY + 1, boxX + 7, boxY + 7, ACCENT);
		}
	}

	private void drawNumberRow(DrawContext context, Row row, NumSetting setting, boolean hover) {
		context.fill(row.x, row.y, row.x + row.w, row.y + row.h, hover ? ROW_HOVER : ROW);
		Draw.text(context, setting.name, row.x + 5, row.y + 3, TEXT);
		String value = setting.display();
		Draw.text(context, value, row.x + row.w - 5 - Draw.width(value), row.y + 3, DIM);
		int barX = row.x + 5;
		int barWidth = row.w - 10;
		int barY = row.y + 15;
		context.fill(barX, barY, barX + barWidth, barY + 3, TRACK);
		int filled = (int) Math.round(barWidth * setting.normalized());
		context.fill(barX, barY, barX + filled, barY + 3, ACCENT);
		context.fill(barX + filled - 1, barY - 1, barX + filled + 2, barY + 4, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		layout();
		double mx = click.x();
		double my = click.y();
		int button = click.button();
		for (Row row : rows) {
			if (!row.hit(mx, my)) {
				continue;
			}
			if (row.header != null) {
				return true;
			}
			if (row.setting == null) {
				if (button == 0) {
					row.module.toggle();
					Config.save();
				} else if (button == 1 && !row.module.settings.isEmpty()) {
					if (!expanded.remove(row.module)) {
						expanded.add(row.module);
					}
				}
				return true;
			}
			if (button == 0) {
				if (row.setting instanceof BoolSetting bool) {
					bool.toggle();
					Config.save();
				} else if (row.setting instanceof NumSetting number) {
					dragging = row;
					applySlider(number, row, mx);
				}
			}
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (dragging != null && dragging.setting instanceof NumSetting number) {
			applySlider(number, dragging, click.x());
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragging != null) {
			dragging = null;
			Config.save();
			return true;
		}
		return super.mouseReleased(click);
	}

	private static void applySlider(NumSetting setting, Row row, double mouseX) {
		double t = (mouseX - (row.x + 5)) / (double) (row.w - 10);
		setting.setNormalized(Math.max(0.0, Math.min(1.0, t)));
	}
}
