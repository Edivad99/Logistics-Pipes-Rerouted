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
            list.add(freshManager(parentChassis));
        }
        return list;
    }

    private static ModuleUpgradeManager freshManager(PipeLogisticsChassis parentChassis) {
        return new ModuleUpgradeManager(parentChassis, parentChassis.getOriginalUpgradeManager());
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
        return freshManager(parentChassis);
    }

    @Override
    public ModuleUpgradeManager readSingleFromNBT(ValueInput input, String key) {
        ModuleUpgradeManager manager = freshManager(parentChassis);
        input.child(key).ifPresent(child -> manager.deserialize(child, ""));
        return manager;
    }

    @Override
    public void writeSingleToNBT(ValueOutput output, String key, ModuleUpgradeManager value) {
        value.serialize(output.child(key), "");
    }

    @Override
    public UpgradeManagerListProperty copyProperty() {
        return new UpgradeManagerListProperty(parentChassis, tagKey, copyValue());
    }
}
