package logisticspipes.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class FinalCompoundTag extends CompoundTag {

	private boolean constructing;

	public FinalCompoundTag(CompoundTag base) {
		super();
		constructing = true;
		super.merge(base);
		constructing = false;
	}

	@Override
	public Set<String> getAllKeys() {
		return Collections.unmodifiableSet(super.getAllKeys());
	}

	@Override
	public void putBoolean(String key, boolean value) {
		if (constructing) super.putBoolean(key, value);
	}

	@Override
	public void putByte(String key, byte value) {
		if (constructing) super.putByte(key, value);
	}

	@Override
	public void putByteArray(String key, byte[] value) {
		if (constructing) super.putByteArray(key, value);
	}

	@Override
	public void putDouble(String key, double value) {
		if (constructing) super.putDouble(key, value);
	}

	@Override
	public void putFloat(String key, float value) {
		if (constructing) super.putFloat(key, value);
	}

	@Override
	public void putIntArray(String key, int[] value) {
		if (constructing) super.putIntArray(key, value);
	}

	@Override
	public void putInt(String key, int value) {
		if (constructing) super.putInt(key, value);
	}

	@Override
	public void putLong(String key, long value) {
		if (constructing) super.putLong(key, value);
	}

	@Override
	public void putShort(String key, short value) {
		if (constructing) super.putShort(key, value);
	}

	@Override
	public void putString(String key, String value) {
		if (constructing) super.putString(key, value);
	}

	@Nullable
	@Override
	public Tag put(String key, Tag value) {
		if (constructing) return super.put(key, value);
		return null;
	}

	@Override
	public void putUUID(String key, UUID value) {
		if (constructing) super.putUUID(key, value);
	}

	@Override
	public CompoundTag merge(CompoundTag other) {
		if (constructing) return super.merge(other);
		return this;
	}

	@Override
	public void remove(String key) {
		if (constructing) super.remove(key);
	}

}
