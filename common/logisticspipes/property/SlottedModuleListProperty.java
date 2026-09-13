/*
 * Copyright (c) 2022  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2022  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular
 * MIT license in your project, replace this copyright notice (this line and any
 * lines below and NOT the copyright line above) with the lines from the original
 * MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this file and associated documentation files (the "Source Code"), to deal in
 * the Source Code without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Source Code, and to permit persons to whom the Source Code is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Source Code, which also can be
 * distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
        String moduleName = slottedModuleInput.getString(SlottedModule.MODULE_NAME_KEY).orElse(null);
        if (moduleName == null) {
            return list.get(slot);
        }
        Identifier moduleResource = LPItems.modules.get(moduleName);
        if (moduleResource == null) {
            return list.get(slot);
        }
        if (!(BuiltInRegistries.ITEM.getValue(moduleResource) instanceof ItemModule itemModule)) {
            return list.get(slot);
        }
        // FIXME: move module creation to before deserialize
        LogisticsModule module = itemModule.getModule(null, null, null);
        if (module == null) {
            return list.get(slot);
        }
        module.deserialize(slottedModuleInput);
        SlottedModule slottedModule = new SlottedModule(slot, module);
        list.set(slot, slottedModule);
        return slottedModule;
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, SlottedModule value) {
        ValueOutput slottedModuleOutput = output.child(key);
        @Nullable LogisticsModule module = value.module();
        if (module != null) {
            module.serialize(slottedModuleOutput);
        }
        slottedModuleOutput.putInt(SlottedModule.SLOT_INDEX_KEY, value.slot());
        if (module != null) {
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
        copy.addAll(list);
        return copy;
    }

    public SlottedModule set(int slot, LogisticsModule module) {
        return set(slot, new SlottedModule(slot, module));
    }

    public SlottedModule clear(int slot) {
        return set(slot, new SlottedModule(slot, null));
    }
}
