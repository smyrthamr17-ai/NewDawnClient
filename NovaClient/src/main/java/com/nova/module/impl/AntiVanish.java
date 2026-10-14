package com.nova.module.impl;

import com.nova.module.BoolSetting;
import com.nova.module.Module;
import com.nova.module.NumSetting;
import com.nova.render.Draw;
import com.nova.util.ServerPing;
import net.minecraft.client.gui.DrawContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compares the player count the server reports in its status ping with the number of players
 * in the tab list. Players that are vanished are counted by the server but not shown in the tab list.
 * Only the number is known, not who they are or where.
 */
public class AntiVanish extends Module {
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Nova-Ping");
		thread.setDaemon(true);
		return thread;
	});

	private final NumSetting interval = add(new NumSetting("Refresh (sec)", 10, 3, 60, 1));
	private final BoolSetting details = add(new BoolSetting("Show Details", true));

	private volatile int pingCount = -1;
	private volatile String status = "waiting";
	private volatile boolean busy;
	private int countdown;
	private String lastAddress = "";

	public AntiVanish() {
		super("Anti Vanish", "Shows how many players are vanished (server ping count minus tab list count).", Category.RENDER);
	}

	@Override
	protected void onEnable() {
		countdown = 0;
	}

	@Override
	protected void onDisable() {
		pingCount = -1;
		status = "waiting";
	}

	@Override
	public void onTick() {
		var handler = mc().getNetworkHandler();
		if (handler == null) {
			return;
		}
		var info = handler.getServerInfo();
		if (info == null || info.address == null) {
			pingCount = -1;
			status = "not on a server";
			return;
		}
		final String address = info.address;
		if (!address.equals(lastAddress)) {
			lastAddress = address;
			pingCount = -1;
			countdown = 0;
		}
		if (countdown > 0) {
			countdown--;
			return;
		}
		if (busy) {
			return;
		}
		countdown = (int) (interval.get() * 20);
		busy = true;
		status = "pinging";
		EXECUTOR.submit(() -> {
			try {
				pingCount = ServerPing.fetchOnline(address);
				status = "ok";
			} catch (Exception e) {
				status = "ping failed";
			} finally {
				busy = false;
			}
		});
	}

	@Override
	public void onHud(DrawContext context, float tickProgress) {
		var handler = mc().getNetworkHandler();
		if (handler == null) {
			return;
		}
		int tab = handler.getListedPlayerListEntries().size();
		int ping = pingCount;
		String text;
		int color;
		if (ping < 0) {
			text = "Vanished: ?  (" + status + ")";
			color = 0xFFAAAAAA;
		} else {
			int hidden = Math.max(0, ping - tab);
			text = "Vanished: " + hidden;
			color = hidden > 0 ? 0xFFFF5555 : 0xFF55FF55;
			if (details.get()) {
				text = text + "  (ping " + ping + " / tab " + tab + ")";
			}
		}
		Draw.text(context, text, 6, 18, color);
	}
}
