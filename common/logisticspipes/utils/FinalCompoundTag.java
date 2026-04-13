package logisticspipes.utils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
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

	@Nonnull
	@Override
	public Set<String> getAllKeys() {
		return Collections.unmodifiableSet(super.getAllKeys());
	}

	@Override
	public void putBoolean(@Nonnull String key, boolean value) {
		if (constructing) super.putBoolean(key, value);
	}

	@Override
	public void putByte(@Nonnull String key, byte value) {
		if (constructing) super.putByte(key, value);
	}

	@Override
	public void putByteArray(@Nonnull String key, @Nonnull byte[] value) {
		if (constructing) super.putByteArray(key, value);
	}

	@Override
	public void putDouble(@Nonnull String key, double value) {
		if (constructing) super.putDouble(key, value);
	}

	@Override
	public void putFloat(@Nonnull String key, float value) {
		if (constructing) super.putFloat(key, value);
	}

	@Override
	public void putIntArray(@Nonnull String key, @Nonnull int[] value) {
		if (constructing) super.putIntArray(key, value);
	}

	@Override
	public void putInt(@Nonnull String key, int value) {
		if (constructing) super.putInt(key, value);
	}

	@Override
	public void putLong(@Nonnull String key, long value) {
		if (constructing) super.putLong(key, value);
	}

	@Override
	public void putShort(@Nonnull String key, short value) {
		if (constructing) super.putShort(key, value);
	}

	@Override
	public void putString(@Nonnull String key, String value) {
		if (constructing) super.putString(key, value);
	}

	@Nullable
	@Override
	public Tag put(@Nonnull String key, @Nonnull Tag value) {
		if (constructing) return super.put(key, value);
		return null;
	}

	@Override
	public void putUUID(@Nonnull String key, @Nonnull UUID value) {
		if (constructing) super.putUUID(key, value);
	}

	@Override
	public CompoundTag merge(@Nonnull CompoundTag other) {
		if (constructing) return super.merge(other);
		return this;
	}

	@Override
	public void remove(@Nonnull String key) {
		if (constructing) super.remove(key);
	}

}
