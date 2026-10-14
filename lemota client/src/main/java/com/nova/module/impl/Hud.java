package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import com.nova.module.ModuleManager;
import com.nova.render.Draw;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Hud extends Module {
	private final BoolSetting watermark = add(new BoolSetting("Watermark", true));
	private final BoolSetting moduleList = add(new BoolSetting("Module List", true));

	public Hud() {
		super("HUD", "Watermark and the list of enabled modules.", Category.RENDER);
	}

	private final List<String> cachedNames = new ArrayList<>();
	private final List<Integer> cachedWidths = new ArrayList<>();
	private long cachedMask = -1;

	private void refreshList() {
		long mask = 0;
		int i = 0;
		for (Module module : ModuleManager.all()) {
			if (module != this && module.isEnabled()) {
				mask |= 1L << i;
			}
			i++;
		}
		if (mask == cachedMask) {
			return;
		}
		cachedMask = mask;
		List<String> names = new ArrayList<>();
		for (Module module : ModuleManager.all()) {
			if (module != this && module.isEnabled()) {
				names.add(module.name);
			}
		}
		names.sort(Comparator.comparingInt((String n) -> Draw.width(n)).reversed());
		cachedNames.clear();
		cachedWidths.clear();
		for (String n : names) {
			cachedNames.add(n);
			cachedWidths.add(Draw.width(n));
		}
	}

	@Override
	public void onHud(DrawContext context, float tickProgress) {
		if (watermark.get()) {
			Draw.text(context, "Nova Client", 6, 6, 0xFFB388FF);
		}
		if (moduleList.get()) {
			refreshList();
			int right = mc().getWindow().getScaledWidth() - 5;
			int y = 5;
			for (int i = 0; i < cachedNames.size(); i++) {
				Draw.text(context, cachedNames.get(i), right - cachedWidths.get(i), y, 0xFFE0D4FF);
				y += 10;
			}
		}
	}
}
