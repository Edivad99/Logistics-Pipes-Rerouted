package logisticspipes.world.level.block.entity;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.network.to_client.block.CompilerStatusMessage;
import logisticspipes.LPConfigs;
import logisticspipes.LPConstants;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.CoordinateUtils;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.SimpleStackInventory;
import logisticspipes.world.inventory.ProgramCompilerMenu;
import logisticspipes.world.item.component.LPDataComponents;

public class LogisticsProgramCompilerBlockEntity extends LogisticsSolidBlockEntity
    implements IScreenOpenController, MenuProvider {

    public static class ProgramCategories {

        public static final Identifier BASIC = LPConstants.rl("compilercategory.basic");
        public static final Identifier TIER_2 = LPConstants.rl("compilercategory.tier_2");
        public static final Identifier FLUID = LPConstants.rl("compilercategory.fluid");
        public static final Identifier TIER_3 = LPConstants.rl("compilercategory.tier_3");
        public static final Identifier CHASSIS = LPConstants.rl("compilercategory.chassis");
        public static final Identifier CHASSIS_2 = LPConstants.rl("compilercategory.chassis_2");
        public static final Identifier CHASSIS_3 = LPConstants.rl("compilercategory.chassis_3");
        public static final Identifier MODDED = LPConstants.rl("compilercategory.modded");

        static {
            //Force the order of keys
            programByCategory.put(BASIC, new HashSet<>());
            programByCategory.put(TIER_2, new HashSet<>());
            programByCategory.put(FLUID, new HashSet<>());
            programByCategory.put(TIER_3, new HashSet<>());
            programByCategory.put(CHASSIS, new HashSet<>());
            programByCategory.put(CHASSIS_2, new HashSet<>());
            programByCategory.put(CHASSIS_3, new HashSet<>());
            programByCategory.put(MODDED, new HashSet<>());
        }
    }

    public LogisticsProgramCompilerBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.PROGRAM_COMPILER.get(), pos, state);
    }

    private static final int DISK_SLOT = 0;
    private static final int PROGRAMMER_SLOT = 1;

    public static final Map<Identifier, Set<Identifier>> programByCategory = new LinkedHashMap<>();
    private final PlayerCollectionList playerList = new PlayerCollectionList();
    /**
     * What the compiler is working towards. Not persisted: an interrupted task starts over.
     *
     * @param progressPerTick how fast this kind of task advances per unit of power consumed
     */
    public enum CompilerTask {
        /** Unlocks a whole category of programs on the disk. */
        CATEGORY(0.0005),
        /** Unlocks one program on the disk. */
        PROGRAM(0.0025),
        /** Writes a program onto the programmer in the slot. */
        FLASH(0.01);

        private final double progressPerTick;

        CompilerTask(double progressPerTick) {
            this.progressPerTick = progressPerTick;
        }
    }

    private @Nullable CompilerTask taskType = null;
    @Getter
    private @Nullable Identifier currentTask = null;
    @Getter
    private double taskProgress = 0;
    @Getter
    private boolean wasAbleToConsumePower = false;

    @Getter
    private final SimpleStackInventory inventory = new SimpleStackInventory(2, "programcompilerinv", 64);

    public ListTag getListTagForKey(String key) {
        ItemStack stack = this.getInventory().getItem(DISK_SLOT);
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag nbt = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            return nbt.getListOrEmpty(key);
        } else {
            return new ListTag();
        }
    }

    public void triggerNewTask(Identifier category, CompilerTask taskType) {
        if (currentTask != null) {
            return;
        }
        this.taskType = taskType;
        currentTask = category;
        taskProgress = 0;
        wasAbleToConsumePower = true;
        updateClient();
    }

    @Override
    public void screenOpenedByPlayer(Player player) {
        playerList.add(player);
    }

    @Override
    public void screenClosedByPlayer(Player player) {
        playerList.remove(player);
    }

    private void addStringToDiskList(String key, String value) {
        ItemStack stack = getInventory().getItem(DISK_SLOT);
        stack.update(
            DataComponents.CUSTOM_DATA,
            CustomData.EMPTY,
            customData -> {
                CompoundTag tag = customData.copyTag();
                ListTag list = tag.getListOrEmpty(key);
                StringTag string = StringTag.valueOf(value);
                if (!list.contains(string)) {
                    list.add(string);
                }
                tag.put(key, list);
                return CustomData.of(tag);
            }
        );
    }

    @Override
    public void update() {
        super.update();
        if (!MainProxy.isServer(this.level)) {
            return;
        }
        if (currentTask == null) {
            return;
        }
        wasAbleToConsumePower = false;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue;
            }
            DoubleCoordinates pos = CoordinateUtils.add(new DoubleCoordinates(this), dir);
            BlockEntity tile = pos.getTileEntity(this.level);
            if (!(tile instanceof LogisticsTileGenericPipe tPipe)) {
                continue;
            }
            if (!(tPipe.pipe.getClass() == PipeItemsBasicLogistics.class)) {
                continue;
            }
            CoreRoutedPipe pipe = (CoreRoutedPipe) tPipe.pipe;
            if (pipe.useEnergy(10)) {
                taskProgress += taskType.progressPerTick * LPConfigs.COMMON.COMPILER_SPEED.getAsDouble();
                wasAbleToConsumePower = true;
            }
            break;
        }

        if (taskProgress >= 1) {
            switch (taskType) {
                case CATEGORY -> addStringToDiskList("compilerCategories", currentTask.toString());
                case PROGRAM -> addStringToDiskList("compilerPrograms", currentTask.toString());
                case FLASH -> {
                    ItemStack programmer = getInventory().getItem(PROGRAMMER_SLOT);
                    if (!programmer.isEmpty()) {
                        programmer.set(LPDataComponents.RECIPE_TARGET, currentTask.toString());
                    }
                }
            }

            taskType = null;
            currentTask = null;
            taskProgress = 0;
            wasAbleToConsumePower = false;
        }
        updateClient();
    }

    public void updateClient() {
        playerList.send(new CompilerStatusMessage(
            getBlockPos(),
            Optional.ofNullable(currentTask),
            taskProgress,
            wasAbleToConsumePower,
            getInventory().getItem(0),
            getInventory().getItem(PROGRAMMER_SLOT)));
    }

    @Override
    public void onBlockBreak() {
        inventory.dropContents(level, getBlockPos());
    }

    public void setStateOnClient(CompilerStatusMessage status) {
        getInventory().setItem(0, status.disk());
        getInventory().setItem(PROGRAMMER_SLOT, status.programmer());
        currentTask = status.currentTask().orElse(null);
        taskProgress = status.progress();
        wasAbleToConsumePower = status.hadPower();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input, "programcompilerinv");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output, "programcompilerinv");
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("FIXME");
    }

    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ProgramCompilerMenu(i, inventory, this);
    }
}
