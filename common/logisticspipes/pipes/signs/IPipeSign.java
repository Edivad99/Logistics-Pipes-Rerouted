package logisticspipes.pipes.signs;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import com.mojang.blaze3d.pipeline.RenderTarget; // was net.minecraft.client.shader.Framebuffer
import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.renderer.blockentity.LogisticsRenderPipe;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public interface IPipeSign extends ValueIOSerializable {

	// Methods used when assigning a sign
	boolean isAllowedFor(CoreRoutedPipe pipe);

	void addSignTo(CoreRoutedPipe pipe, Direction dir, Player player);


	void init(CoreRoutedPipe pipe, Direction dir);

	void activate(Player player);

	/** The message that tells clients what this sign shows, or null when it has nothing to say. */
	@Nullable
	CustomPacketPayload getPacket();

	void updateServerSide();

	void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, SubmitNodeCollector collector, int packedLight);

	RenderTarget getMCFrameBufferForSign(); // was Framebuffer

	boolean doesFrameBufferNeedUpdating(CoreRoutedPipe pipe, LogisticsRenderPipe renderer);
}
