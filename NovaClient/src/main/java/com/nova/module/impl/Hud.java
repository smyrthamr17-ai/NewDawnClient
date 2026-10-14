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

	@Override
	public void onHud(DrawContext context, float tickProgress) {
		if (watermark.get()) {
			Draw.text(context, "Nova Client", 6, 6, 0xFFB388FF);
		}
		if (moduleList.get()) {
			List<String> names = new ArrayList<>();
			for (Module module : ModuleManager.all()) {
				if (module != this && module.isEnabled()) {
					names.add(module.name);
				}
			}
			names.sort(Comparator.comparingInt((String s) -> Draw.width(s)).reversed());
			int right = mc().getWindow().getScaledWidth() - 5;
			int y = 5;
			for (String name : names) {
				Draw.text(context, name, right - Draw.width(name), y, 0xFFE0D4FF);
				y += 10;
			}
		}
	}
}
