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

package network.rs485.logisticspipes.property;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.property.ObserverCallback;
import logisticspipes.api.property.Property;
import logisticspipes.api.property.PropertyUtil;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.module.ModulePropertiesMessage;
import network.rs485.grow.Coroutines;

public class PropertyUpdater implements Consumer<Property<?>> {

    private final WeakReference<Player> weakPlayer;

    // One stable instance: a fresh method reference per call site would never compare equal
    // when removing the observer again.
    private final ObserverCallback<Object> observer = this::accept;

    private final List<Property<?>> properties;
    private final Set<Property<?>> changedProperties = new HashSet<>();
    private final LogisticsModule module;

    private boolean shouldUpdate = false;

    public PropertyUpdater(Player player, LogisticsModule moduleIn, List<Property<?>> propertiesIn) {
        this.weakPlayer = new WeakReference<>(player);
        this.properties = propertiesIn;
        this.module = moduleIn;
        PropertyUtil.addObserver(propertiesIn, observer);
    }

    @Override
    public void accept(Property<?> property) {
        changedProperties.add(property);
        if (!shouldUpdate) {
            shouldUpdate = true;
            Coroutines.INSTANCE.scheduleServerTask(5, this::sendPropertyUpdate);
        }
    }

    private void sendPropertyUpdate() {
        if (!shouldUpdate || weakPlayer.isEnqueued()) {
            return;
        }
        @Nullable Player player = weakPlayer.get();
        if (player == null) {
            return;
        }
        TagValueOutput output =
            TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
        PropertyUtil.serialize(changedProperties, output);
        changedProperties.clear();
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                serverPlayer,
                new ModulePropertiesMessage(ModuleTarget.of(module), output.buildResult()));
        }
        shouldUpdate = false;
    }

    boolean removeForPlayer(Player entityPlayer) {
        boolean shouldBeRemoved = weakPlayer.isEnqueued()
            || weakPlayer.get() == null
            || weakPlayer.get() == entityPlayer;
        if (shouldBeRemoved) {
            PropertyUtil.removeObserver(properties, observer);
            shouldUpdate = false;
        }
        return shouldBeRemoved;
    }
}
