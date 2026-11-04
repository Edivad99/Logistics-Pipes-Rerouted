package logisticspipes.data.gametest.instances;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;

/**
 * An explosion goes off on a stack of item cards lying on the ground. With an ender pearl beside
 * them, blank cards survive it all on one new frequency and the pearl is used up; without one, or
 * on cards that already have a frequency, it destroys them like any other item.
 */
public class ItemCardLinkingTestInstance extends GameTestInstance {

    public static final MapCodec<ItemCardLinkingTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, ItemCardLinkingTestInstance::new));

    private static final Vec3 CARDS = new Vec3(2.5, 1, 2.5);
    private static final Vec3 PEARL = new Vec3(2.5, 1, 3.0);
    private static final int COUNT = 3;

    private final Scenario scenario;

    public ItemCardLinkingTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        var level = helper.getLevel();
        var cards = new ItemStack(LPItems.ITEM_CARD.get(), COUNT);
        if (scenario == Scenario.ALREADY_LINKED) {
            cards.set(LPDataComponents.UUID, UUID.randomUUID());
        }
        spawn(helper, CARDS, cards);
        if (scenario != Scenario.NO_PEARL) {
            spawn(helper, PEARL, new ItemStack(Items.ENDER_PEARL));
        }

        Vec3 center = helper.absoluteVec(CARDS);
        helper.startSequence()
            .thenExecute(() -> level.explode(null, center.x, center.y, center.z, 2.0F, Level.ExplosionInteraction.NONE))
            .thenExecuteAfter(1, () -> {
                var items = helper.getEntities(EntityType.ITEM);
                if (scenario == Scenario.WITH_PEARL) {
                    helper.assertTrue(items.stream().noneMatch(item -> item.getItem().is(Items.ENDER_PEARL)),
                        Component.literal("The ender pearl should have been used up"));
                    int count = items.stream().mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(count == COUNT,
                        Component.literal("Expected %d linked cards, found %d".formatted(COUNT, count)));
                    UUID frequency = items.getFirst().getItem().get(LPDataComponents.UUID);
                    helper.assertTrue(frequency != null && items.stream().allMatch(item ->
                            item.getItem().is(LPItems.ITEM_CARD.get())
                                && Objects.equals(item.getItem().get(LPDataComponents.UUID), frequency)),
                        Component.literal("The cards should all share one frequency"));
                } else {
                    helper.assertTrue(items.stream().noneMatch(item -> item.getItem().is(LPItems.ITEM_CARD.get())),
                        Component.literal("The explosion should have destroyed the cards"));
                }
            })
            .thenSucceed();
    }

    private static void spawn(GameTestHelper helper, Vec3 relative, ItemStack stack) {
        Vec3 position = helper.absoluteVec(relative);
        var item = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, stack);
        item.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(item);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Item card linking: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Blank cards blown up with an ender pearl.
         */
        WITH_PEARL,
        /**
         * Blank cards blown up on their own.
         */
        NO_PEARL,
        /**
         * Cards that already have a frequency, blown up with an ender pearl.
         */
        ALREADY_LINKED;

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
