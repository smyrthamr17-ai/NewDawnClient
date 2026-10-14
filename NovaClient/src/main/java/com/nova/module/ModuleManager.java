package com.nova.module;

import com.nova.module.impl.AntiVanish;
import com.nova.module.impl.AutoMine;
import com.nova.module.impl.AutoWalk;
import com.nova.module.impl.Hud;
import com.nova.module.impl.PlayerEsp;
import com.nova.module.impl.StorageEsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {
	}

	public static void init() {
		if (!MODULES.isEmpty()) {
			return;
		}
		Hud hud = new Hud();
		MODULES.add(hud);
		MODULES.add(new PlayerEsp());
		MODULES.add(new StorageEsp());
		MODULES.add(new AntiVanish());
		MODULES.add(new AutoWalk());
		MODULES.add(new AutoMine());
		hud.setEnabled(true);
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> byCategory(Module.Category category) {
		List<Module> result = new ArrayList<>();
		for (Module m : MODULES) {
			if (m.category == category) {
				result.add(m);
			}
		}
		return result;
	}
}
