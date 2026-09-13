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
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.property.ValueProperty;
import logisticspipes.connection.DynamicAdjacent;
import logisticspipes.connection.NoAdjacent;
import logisticspipes.connection.SingleAdjacent;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.DirectionUtil;

public class AdjacentProperty extends ValueProperty<Adjacent> {

    private final CoreRoutedPipe pipe;
    private final String tagKey;

    public AdjacentProperty(CoreRoutedPipe pipe, String tagKey) {
        this(NoAdjacent.INSTANCE, pipe, tagKey);
    }

    public AdjacentProperty(Adjacent defaultValue, CoreRoutedPipe pipe, String tagKey) {
        super(defaultValue);
        this.pipe = pipe;
        this.tagKey = tagKey;
    }

    @Override
    public String getTagKey() {
        return tagKey;
    }

    @Override
    public Adjacent copyValue() {
        return getValue(); // Adjacent is immutable
    }

    @Override
    public AdjacentProperty copyProperty() {
        return new AdjacentProperty(getValue(), pipe, tagKey);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.list(tagKey, Codec.STRING).ifPresent(storedConnections -> {
            List<String> stored = storedConnections.stream().toList();
            assert stored.size() <= 6;
            if (stored.isEmpty()) {
                setValue(NoAdjacent.INSTANCE);
                return;
            }
            List<String> adjacentConnections = new ArrayList<>(6);
            for (int idx = 0; idx < 6; idx++) {
                adjacentConnections.add(idx < stored.size() ? stored.get(idx) : "");
            }
            List<Integer> activeIndices = new ArrayList<>(6);
            for (int idx = 0; idx < 6; idx++) {
                if (!adjacentConnections.get(idx).isBlank()) {
                    activeIndices.add(idx);
                }
            }
            if (activeIndices.isEmpty()) {
                setValue(NoAdjacent.INSTANCE);
            } else if (activeIndices.size() == 1) {
                int idx = activeIndices.getFirst();
                Direction direction = DirectionUtil.getOrientation(idx);
                assert direction != null;
                setValue(new SingleAdjacent(pipe, direction,
                    ConnectionType.valueOf(adjacentConnections.get(idx))));
            } else {
                @Nullable ConnectionType[] cache = new ConnectionType[6];
                for (int idx = 0; idx < 6; idx++) {
                    String name = adjacentConnections.get(idx);
                    cache[idx] = name.isBlank() ? null : ConnectionType.valueOf(name);
                }
                setValue(new DynamicAdjacent(pipe, cache));
            }
        });
    }

    @Override
    public void serialize(ValueOutput output) {
        ValueOutput.TypedOutputList<String> list = output.list(tagKey, Codec.STRING);
        if (getValue() == NoAdjacent.INSTANCE) {
            return;
        }
        for (Direction dir : Direction.values()) {
            ConnectionType connectionType = getValue().get(dir);
            list.add(connectionType == null ? "" : connectionType.name());
        }
    }

    public @Nullable Direction getDirectionOrNull() {
        return getValue() instanceof SingleAdjacent singleAdjacent ? singleAdjacent.getDir() : null;
    }
}
