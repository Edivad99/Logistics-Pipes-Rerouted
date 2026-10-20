package logisticspipes.world.attachment;

import java.util.function.Supplier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.serialization.Codec;

import logisticspipes.LPConstants;

public class LPAttachmentTypes {

    private static final DeferredRegister<AttachmentType<?>> deferredRegister =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }

    public static final Supplier<AttachmentType<Boolean>> RECEIVED_GUIDE_BOOK =
        deferredRegister.register(
            "received_guide_book",
            () -> AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL.fieldOf("received"), received -> received)
                .copyOnDeath()
                .build()
        );
}
