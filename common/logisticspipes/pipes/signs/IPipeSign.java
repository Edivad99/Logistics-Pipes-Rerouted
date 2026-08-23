package logisticspipes.pipes.signs;

import net.neoforged.neoforge.common.util.ValueIOSerializable;
import com.mojang.blaze3d.pipeline.RenderTarget; // was net.minecraft.client.shader.Framebuffer
import com.mojang.blaze3d.vertex.PoseStack;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.client.renderer.blockentity.LogisticsRenderPipe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPipeSign extends ValueIOSerializable {

	// Methods used when assigning a sign
	boolean isAllowedFor(CoreRoutedPipe pipe);

	void addSignTo(CoreRoutedPipe pipe, Direction dir, Player player);


	void init(CoreRoutedPipe pipe, Direction dir);

	void activate(Player player);

	ModernPacket getPacket();

	void updateServerSide();

	void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight);

	RenderTarget getMCFrameBufferForSign(); // was Framebuffer

	boolean doesFrameBufferNeedUpdating(CoreRoutedPipe pipe, LogisticsRenderPipe renderer);
}
