package logisticspipes.integrations.computercraft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import dan200.computercraft.api.lua.LuaException;

import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Items as Lua sees them: a table with {@code name}, {@code displayName} and {@code count}, plus a
 * {@code fingerprint} when the item carries components. Scripts name an item either by its id or by
 * passing back a table they were given.
 */
final class LuaItems {

    private LuaItems() {
    }

    static Map<String, Object> toLua(ItemIdentifier item, int count) {
        Map<String, Object> table = new HashMap<>();
        table.put("name", BuiltInRegistries.ITEM.getKey(item.item).toString());
        table.put("displayName", item.getFriendlyName());
        table.put("count", count);
        if (!item.components.isEmpty()) {
            table.put("fingerprint", fingerprint(item));
        }
        return table;
    }

    static Map<String, Object> toLua(ItemIdentifierStack stack) {
        return toLua(stack.getItem(), stack.getStackSize());
    }

    /**
     * Tells apart items that share an id but not their components, the way CC: Tweaked's own item
     * details do with their {@code nbt} hash.
     */
    static String fingerprint(ItemIdentifier item) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(item.describeComponents().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every JVM ships MD5", e);
        }
    }

    /**
     * The item a script means. A bare id, or a table without a fingerprint, is the item with no
     * components. A fingerprint can only be told apart from what the network holds, so
     * {@code candidates} is asked only then.
     */
    static ItemIdentifier resolve(Object spec, Supplier<Collection<ItemIdentifier>> candidates)
        throws LuaException {
        String name;
        Object fingerprint = null;
        if (spec instanceof String string) {
            name = string;
        } else if (spec instanceof Map<?, ?> table && table.get("name") instanceof String string) {
            name = string;
            fingerprint = table.get("fingerprint");
        } else {
            throw new LuaException("Expected an item name or an item table");
        }

        Identifier id = Identifier.tryParse(name);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            throw new LuaException("Unknown item " + name);
        }
        if (fingerprint == null) {
            return ItemIdentifier.get(item);
        }
        for (ItemIdentifier candidate : candidates.get()) {
            if (candidate.item == item && fingerprint.equals(fingerprint(candidate))) {
                return candidate;
            }
        }
        throw new LuaException("No " + name + " with fingerprint " + fingerprint + " in the network");
    }
}
