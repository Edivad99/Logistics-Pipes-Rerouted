package logisticspipes.world.item;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.pipe.NormalOrdererGui;
import logisticspipes.network.to_client.pipe.RemoteOrdererDimensionMessage;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.world.item.component.LPDataComponents;

public class RemoteOrderer extends LogisticsItem {

    public RemoteOrderer(Properties properties) {
        super(properties);
    }

    public static void connectToPipe(ItemStack stack, PipeItemsRemoteOrdererLogistics pipe) {
        stack.set(LPDataComponents.CONNECTED_PIPE, GlobalPos.of(
            Objects.requireNonNull(pipe.getWorld()).dimension(),
            Objects.requireNonNull(pipe.getPos())
        ));
    }

    public static @Nullable PipeItemsRemoteOrdererLogistics getPipe(MinecraftServer server, ItemStack stack) {
        GlobalPos connectedPipe = stack.get(LPDataComponents.CONNECTED_PIPE);
        if (connectedPipe == null) {
            return null;
        }
        ServerLevel level = server.getLevel(connectedPipe.dimension());
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(connectedPipe.pos());
        if (!(be instanceof LogisticsTileGenericPipe genericPipe)) {
            return null;
        }
        if (genericPipe.pipe instanceof PipeItemsRemoteOrdererLogistics remoteOrdererLogistics) {
            return remoteOrdererLogistics;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        if (stack.has(LPDataComponents.CONNECTED_PIPE)) {
            tooltipAdder.accept(Component.literal("Has Remote Pipe"));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand handIn) {
        ItemStack par1ItemStack = player.getMainHandItem();
        if (par1ItemStack.isEmpty() || !par1ItemStack.has(LPDataComponents.CONNECTED_PIPE)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            PipeItemsRemoteOrdererLogistics pipe = RemoteOrderer.getPipe(serverLevel.getServer(), par1ItemStack);
            if (pipe != null) {
                int energyUse = 0;
                if (pipe.getWorld() != level) {
                    energyUse += 2500;
                }
                energyUse = (int) (energyUse + Math.sqrt(Math.pow(pipe.getX() - player.getX(), 2) +
                    Math.pow(pipe.getY() - player.getY(), 2) +
                    Math.pow(pipe.getZ() - player.getZ(), 2)
                ));
                if (pipe.useEnergy(energyUse)) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer, new RemoteOrdererDimensionMessage(
                                pipe.getWorld().dimension().identifier()));
                    }
                    NormalOrdererGui gui = NewGuiHandler.getGui(NormalOrdererGui.class);
                    gui.setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ());
                    gui.setDim(pipe.getWorld().dimension().identifier());
                    gui.open(player);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
