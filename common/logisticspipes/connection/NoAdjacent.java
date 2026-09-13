/*
 * Copyright (c) 2021  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the
 * MIT license:
 *
 * Copyright (c) 2021  RS485
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
 * of the Source Code, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
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

package logisticspipes.connection;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import logisticspipes.api.connection.Adjacent;
import logisticspipes.api.connection.ConnectionType;
import logisticspipes.api.connection.NeighborBlockEntity;

public final class NoAdjacent implements Adjacent {

    public static final NoAdjacent INSTANCE = new NoAdjacent();

    private NoAdjacent() {
    }

    @Override
    public Map<BlockPos, ConnectionType> connectedPos() {
        return Collections.emptyMap();
    }

    @Override
    public @Nullable ConnectionType get(Direction direction) {
        return null;
    }

    @Override
    public Optional<ConnectionType> optionalGet(Direction direction) {
        return Optional.empty();
    }

    @Override
    public Map<NeighborBlockEntity<BlockEntity>, ConnectionType> neighbors() {
        return Collections.emptyMap();
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> inventories() {
        return Collections.emptyList();
    }

    @Override
    public List<NeighborBlockEntity<BlockEntity>> fluidTanks() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "NoAdjacent";
    }
}
