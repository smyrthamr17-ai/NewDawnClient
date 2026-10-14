package com.nova.module;

import com.google.gson.JsonObject;

import java.util.Locale;

public class NumSetting extends Setting {
	public final double min;
	public final double max;
	public final double step;
	private double value;

	public NumSetting(String name, double defaultValue, double min, double max, double step) {
		super(name);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = clampAndSnap(defaultValue);
	}

	private double clampAndSnap(double v) {
		double c = Math.max(min, Math.min(max, v));
		if (step > 0) {
			c = min + Math.round((c - min) / step) * step;
		}
		return Math.max(min, Math.min(max, c));
	}

	public double get() {
		return value;
	}

	public int asInt() {
		return (int) Math.round(value);
	}

	public void set(double v) {
		value = clampAndSnap(v);
	}

	/** t is between 0 and 1. */
	public void setNormalized(double t) {
		set(min + (max - min) * t);
	}

	public double normalized() {
		return max == min ? 0.0 : (value - min) / (max - min);
	}

	public String display() {
		if (step >= 1) {
			return Integer.toString((int) Math.round(value));
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}

	@Override
	public void save(JsonObject out) {
		out.addProperty(name, value);
	}

	@Override
	public void load(JsonObject in) {
		if (in.has(name) && in.get(name).isJsonPrimitive()) {
			try {
				set(in.get(name).getAsDouble());
			} catch (RuntimeException ignored) {
				// keep the default value
			}
		}
	}
}
