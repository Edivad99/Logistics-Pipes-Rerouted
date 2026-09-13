/*
 * Copyright (c) 2023  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2023  RS485
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.api.property.ListProperty;
import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.utils.item.SimpleStackInventory;

public class UpgradeManagerListProperty extends ListProperty<ModuleUpgradeManager> {

    private final String tagKey;
    private final PipeLogisticsChassis parentChassis;
    private final ArrayList<Property<?>> subProperties = new ArrayList<>();

    // One stable instance: a fresh method reference per call site would never compare equal
    // when removing the observer again.
    private final ObserverCallback<SimpleStackInventory> contentObserver = prop -> notifyFromContent();

    public UpgradeManagerListProperty(int slots, PipeLogisticsChassis parentChassis, String tagKey) {
        this(parentChassis, tagKey, freshManagers(slots, parentChassis));
    }

    private UpgradeManagerListProperty(PipeLogisticsChassis parentChassis, String tagKey,
        List<ModuleUpgradeManager> list) {
        super(list);
        this.parentChassis = parentChassis;
        this.tagKey = tagKey;
        this.subProperties.ensureCapacity(list.size());
        ensureObservingContents();
    }

    private static List<ModuleUpgradeManager> freshManagers(int slots, PipeLogisticsChassis parentChassis) {
        List<ModuleUpgradeManager> list = new ArrayList<>(slots);
        for (int idx = 0; idx < slots; idx++) {
            list.add(new ModuleUpgradeManager(parentChassis, parentChassis.getOriginalUpgradeManager()));
        }
        return list;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    private void ensureObservingContents() {
        // TODO: check possibility of a generalized ListProperty of Property
        Set<Property<?>> propertiesToUnobserve = new HashSet<>(subProperties);
        for (ModuleUpgradeManager manager : list) {
            if (!propertiesToUnobserve.remove(manager.getInv())) {
                manager.getInv().addObserver(contentObserver);
                subProperties.add(manager.getInv());
            }
        }
        for (Property<?> stale : propertiesToUnobserve) {
            stale.getPropertyObservers().remove(contentObserver);
        }
        subProperties.removeAll(propertiesToUnobserve);
    }

    private void notifyFromContent() {
        super.iChanged();
    }

    @Override
    public void iChanged() {
        super.iChanged();
        ensureObservingContents();
    }

    @Override
    public ModuleUpgradeManager copyValue(ModuleUpgradeManager obj) {
        return new ModuleUpgradeManager(obj);
    }

    @Override
    public ModuleUpgradeManager defaultValue(int idx) {
        return new ModuleUpgradeManager(parentChassis, parentChassis.getOriginalUpgradeManager());
    }

    @Override
    public ModuleUpgradeManager readSingleFromNBT(ValueInput input, String key) {
        ModuleUpgradeManager manager =
            new ModuleUpgradeManager(parentChassis, parentChassis.getOriginalUpgradeManager());
        input.child(key).ifPresent(child -> manager.deserialize(child, ""));
        return manager;
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, ModuleUpgradeManager value) {
        value.serialize(output.child(key), "");
    }

    @Override
    public Property<? extends List<ModuleUpgradeManager>> copyProperty() {
        List<ModuleUpgradeManager> copies = new ArrayList<>(size());
        for (int idx = 0; idx < size(); idx++) {
            copies.add(new ModuleUpgradeManager(get(idx)));
        }
        return new UpgradeManagerListProperty(parentChassis, tagKey, copies);
    }
}
