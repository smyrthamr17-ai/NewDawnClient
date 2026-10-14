package com.nova.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

/**
 * Minimal Minecraft "server list ping". Asks the server how many players it says are online,
 * exactly like the multiplayer menu does. Pure Java, so it does not depend on Minecraft internals.
 */
public final class ServerPing {
	private static final int PROTOCOL_VERSION = 774;

	private ServerPing() {
	}

	/** Returns the "online" player count from the server status. Blocks, so call it from a background thread. */
	public static int fetchOnline(String address) throws IOException {
		String host = address.trim();
		int port = 25565;
		boolean hasPort = false;

		if (host.startsWith("[")) {
			int end = host.indexOf(']');
			if (end > 0) {
				String rest = host.substring(end + 1);
				String inside = host.substring(1, end);
				if (rest.startsWith(":")) {
					port = parsePort(rest.substring(1));
					hasPort = true;
				}
				host = inside;
			}
		} else {
			int colon = host.lastIndexOf(':');
			if (colon > 0 && host.indexOf(':') == colon) {
				port = parsePort(host.substring(colon + 1));
				hasPort = true;
				host = host.substring(0, colon);
			}
		}

		String connectHost = host;
		int connectPort = port;
		if (!hasPort) {
			String[] srv = lookupSrv(host);
			if (srv != null) {
				connectHost = srv[0];
				connectPort = parsePort(srv[1]);
			}
		}

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(connectHost, connectPort), 4000);
			socket.setSoTimeout(4000);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

			// handshake packet (next state = status)
			ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
			DataOutputStream handshake = new DataOutputStream(handshakeBytes);
			writeVarInt(handshake, 0x00);
			writeVarInt(handshake, PROTOCOL_VERSION);
			writeString(handshake, host);
			handshake.writeShort(port);
			writeVarInt(handshake, 1);
			writeVarInt(out, handshakeBytes.size());
			out.write(handshakeBytes.toByteArray());

			// status request packet
			writeVarInt(out, 1);
			writeVarInt(out, 0x00);
			out.flush();

			// status response
			readVarInt(in); // total packet length (not needed)
			int packetId = readVarInt(in);
			if (packetId != 0x00) {
				throw new IOException("Unexpected packet id " + packetId);
			}
			int length = readVarInt(in);
			if (length <= 0 || length > (1 << 22)) {
				throw new IOException("Bad status length " + length);
			}
			byte[] data = new byte[length];
			in.readFully(data);
			String json = new String(data, StandardCharsets.UTF_8);

			try {
				JsonObject root = JsonParser.parseString(json).getAsJsonObject();
				return root.getAsJsonObject("players").get("online").getAsInt();
			} catch (RuntimeException e) {
				throw new IOException("Could not read player count", e);
			}
		}
	}

	private static int parsePort(String s) throws IOException {
		try {
			int p = Integer.parseInt(s.trim());
			if (p < 1 || p > 65535) {
				throw new IOException("Bad port " + p);
			}
			return p;
		} catch (NumberFormatException e) {
			throw new IOException("Bad port " + s, e);
		}
	}

	/** Looks up the _minecraft._tcp SRV record. Returns {host, port} or null. */
	private static String[] lookupSrv(String host) {
		try {
			Hashtable<String, String> env = new Hashtable<>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			env.put("java.naming.provider.url", "dns:");
			env.put("com.sun.jndi.dns.timeout.initial", "3000");
			env.put("com.sun.jndi.dns.timeout.retries", "1");
			DirContext context = new InitialDirContext(env);
			try {
				Attributes attributes = context.getAttributes("_minecraft._tcp." + host, new String[] {"SRV"});
				Attribute record = attributes.get("srv");
				if (record != null && record.size() > 0) {
					String[] parts = record.get(0).toString().split(" ");
					if (parts.length >= 4) {
						String target = parts[3];
						if (target.endsWith(".")) {
							target = target.substring(0, target.length() - 1);
						}
						return new String[] {target, parts[2]};
					}
				}
			} finally {
				context.close();
			}
		} catch (Exception ignored) {
			// no SRV record, use the plain address
		}
		return null;
	}

	private static void writeVarInt(DataOutputStream out, int value) throws IOException {
		int v = value;
		while ((v & ~0x7F) != 0) {
			out.writeByte((v & 0x7F) | 0x80);
			v >>>= 7;
		}
		out.writeByte(v);
	}

	private static int readVarInt(DataInputStream in) throws IOException {
		int result = 0;
		int shift = 0;
		int b;
		do {
			b = in.readUnsignedByte();
			result |= (b & 0x7F) << shift;
			shift += 7;
			if (shift > 35) {
				throw new IOException("VarInt too big");
			}
		} while ((b & 0x80) != 0);
		return result;
	}

	private static void writeString(DataOutputStream out, String s) throws IOException {
		byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
		writeVarInt(out, bytes.length);
		out.write(bytes);
	}
}
