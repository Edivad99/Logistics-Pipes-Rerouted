package logisticspipes.property;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.ListProperty;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.world.item.ItemModule;
import logisticspipes.world.item.LPItems;

public class SlottedModuleListProperty extends ListProperty<SlottedModule> {

    private final String tagKey;

    public SlottedModuleListProperty(int slots, String tagKey) {
        super(emptySlots(slots));
        this.tagKey = tagKey;
    }

    private static List<SlottedModule> emptySlots(int slots) {
        List<SlottedModule> list = new ArrayList<>(slots);
        for (int idx = 0; idx < slots; idx++) {
            list.add(new SlottedModule(idx, null));
        }
        return list;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public SlottedModule defaultValue(int idx) {
        return new SlottedModule(idx, null);
    }

    @Override
    public SlottedModule readSingleFromNBT(ValueInput input, String key) {
        ValueInput slottedModuleInput = input.childOrEmpty(key);
        int slot = slottedModuleInput.getIntOr(SlottedModule.SLOT_INDEX_KEY, 0);
        return new SlottedModule(slot, readModule(slottedModuleInput));
    }

    private static @Nullable LogisticsModule readModule(ValueInput input) {
        Identifier moduleId = input.getString(SlottedModule.MODULE_NAME_KEY)
            .map(LPItems.modules::get)
            .orElse(null);
        if (moduleId == null || !(BuiltInRegistries.ITEM.getValue(moduleId) instanceof ItemModule itemModule)) {
            return null;
        }
        // FIXME: move module creation to before deserialize
        LogisticsModule module = itemModule.getModule(null, null, null);
        if (module != null) {
            module.deserialize(input);
        }
        return module;
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, SlottedModule value) {
        ValueOutput slottedModuleOutput = output.child(key);
        slottedModuleOutput.putInt(SlottedModule.SLOT_INDEX_KEY, value.slot());
        LogisticsModule module = value.module();
        if (module != null) {
            module.serialize(slottedModuleOutput);
            slottedModuleOutput.putString(SlottedModule.MODULE_NAME_KEY, module.getLPName());
        }
    }

    @Override
    public SlottedModule copyValue(SlottedModule obj) {
        return new SlottedModule(obj.slot(), null);
    }

    @Override
    public SlottedModuleListProperty copyProperty() {
        SlottedModuleListProperty copy = new SlottedModuleListProperty(size(), tagKey);
        copy.replaceContent(list);
        return copy;
    }

    public SlottedModule set(int slot, LogisticsModule module) {
        return set(slot, new SlottedModule(slot, module));
    }

    public SlottedModule clear(int slot) {
        return set(slot, new SlottedModule(slot, null));
    }
}
