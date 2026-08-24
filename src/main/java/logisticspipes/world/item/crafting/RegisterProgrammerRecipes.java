package logisticspipes.world.item.crafting;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleEnchantmentSink;
import logisticspipes.modules.ModuleEnchantmentSinkMK2;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.upgrades.ActionSpeedUpgrade;
import logisticspipes.pipes.upgrades.AdvancedSatelliteUpgrade;
import logisticspipes.pipes.upgrades.CombinedSneakyUpgrade;
import logisticspipes.pipes.upgrades.ConnectionUpgradeConfig;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.pipes.upgrades.CraftingCleanupUpgrade;
import logisticspipes.pipes.upgrades.CraftingMonitoringUpgrade;
import logisticspipes.pipes.upgrades.FluidCraftingUpgrade;
import logisticspipes.pipes.upgrades.FuzzyUpgrade;
import logisticspipes.pipes.upgrades.ItemExtractionUpgrade;
import logisticspipes.pipes.upgrades.ItemStackExtractionUpgrade;
import logisticspipes.pipes.upgrades.OpaqueUpgrade;
import logisticspipes.pipes.upgrades.PatternUpgrade;
import logisticspipes.pipes.upgrades.PowerTransportationUpgrade;
import logisticspipes.pipes.upgrades.SneakyUpgradeConfig;
import logisticspipes.pipes.upgrades.SpeedUpgrade;
import logisticspipes.pipes.upgrades.UpgradeModuleUpgrade;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import network.rs485.logisticspipes.module.AsyncExtractorModule;
import network.rs485.logisticspipes.module.AsyncQuicksortModule;

public class RegisterProgrammerRecipes {

    private static void registerUpgradeRecipe(Identifier recipeCategory, String upgradeName) {
        Identifier upgradeResource = LPItems.upgrades.get(upgradeName);
        if (upgradeResource == null) {
            return;
        }
        Item upgrade = BuiltInRegistries.ITEM.getValue(upgradeResource);
        if (upgrade.equals(Items.AIR)) {
            return;
        }

        final Set<Identifier> compilerPrograms = LogisticsProgramCompilerBlockEntity.programByCategory
            .computeIfAbsent(recipeCategory, k -> new HashSet<>());
        compilerPrograms.add(upgradeResource);
    }

    private static void registerPipeRecipeCategory(Identifier recipeCategory, Item targetPipe) {
        if (!LogisticsProgramCompilerBlockEntity.programByCategory.containsKey(recipeCategory)) {
            LogisticsProgramCompilerBlockEntity.programByCategory.put(recipeCategory,
                new HashSet<>());
        }
        LogisticsProgramCompilerBlockEntity.programByCategory.get(recipeCategory)
            .add(BuiltInRegistries.ITEM.getKey(targetPipe));
    }

    private static void registerModuleRecipe(Identifier recipeCategory, String moduleName,
        @Nullable String baseModuleName) {
        final Identifier moduleResource = LPItems.modules.get(moduleName);
        Item module = BuiltInRegistries.ITEM.getValue(moduleResource);
        if (module.equals(Items.AIR)) {
            return;
        }
        Item baseModule;
        if (baseModuleName == null) {
            baseModule = LPItems.MODULE_BLANK.get();
        } else {
            baseModule = BuiltInRegistries.ITEM.getValue(LPItems.modules.get(baseModuleName));
        }
        if (baseModule.equals(Items.AIR)) {
            return;
        }

        final Set<Identifier> compilerPrograms = LogisticsProgramCompilerBlockEntity.programByCategory
            .computeIfAbsent(recipeCategory, k -> new HashSet<>());
        compilerPrograms.add(moduleResource);
    }

    public static void loadRecipes() {
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            SneakyUpgradeConfig.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            SpeedUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            CombinedSneakyUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            ConnectionUpgradeConfig.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            AdvancedSatelliteUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            FluidCraftingUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            CraftingByproductUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            PatternUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            FuzzyUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            PowerTransportationUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            CraftingMonitoringUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            OpaqueUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            CraftingCleanupUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_3,
            UpgradeModuleUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            ActionSpeedUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            ItemExtractionUpgrade.getName());
        registerUpgradeRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            ItemStackExtractionUpgrade.getName());

        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            LPItems.PIPE_REQUEST.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            LPItems.PIPE_PROVIDER.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            LPItems.PIPE_CRAFTING.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            LPItems.PIPE_SATELLITE.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.BASIC,
            LPItems.PIPE_SUPPLIER.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            LPItems.PIPE_REQUEST_MK2.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            LPItems.PIPE_REMOTE_ORDERER.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_3,
            LPItems.PIPE_INV_SYS_CONNECTOR.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            LPItems.PIPE_SYSTEM_ENTRANCE.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_2,
            LPItems.PIPE_SYSTEM_DESTINATION.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.TIER_3,
            LPItems.PIPE_FIREWALL.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            LPItems.PIPE_CHASSIS_MK1.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            LPItems.PIPE_CHASSIS_MK2.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            LPItems.PIPE_CHASSIS_MK3.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            LPItems.PIPE_CHASSIS_MK4.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            LPItems.PIPE_CHASSIS_MK5.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_SUPPLIER.get());

        // PipeFluidBasic and PipeFluidTerminus were removed in the 1.20.1 migration; their
        // LPItems fields are null. LP1 used pipeFluidBasic as the 's' base for the rest of the
        // fluid-pipe line, so here we substitute pipeFluidSupplier (a registered fluid pipe) as
        // the base — mirroring how the pipeFluidSupplierMk2 recipe below is keyed off pipeFluidSupplier.
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_REQUEST.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_PROVIDER.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_SUPPLIER_MK2.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_SATELLITE.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_INSERTION.get());
        registerPipeRecipeCategory(LogisticsProgramCompilerBlockEntity.ProgramCategories.FLUID,
            LPItems.PIPE_FLUID_EXTRACTOR.get());

        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            ModuleItemSink.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            ModulePassiveSupplier.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            AsyncExtractorModule.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            ModulePolymorphicItemSink.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            AsyncQuicksortModule.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            ModuleTerminus.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            AsyncAdvancedExtractor.getName(), AsyncExtractorModule.getName());
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS,
            ModuleProvider.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleModBasedItemSink.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleOreDictItemSink.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleEnchantmentSink.getName(), ModuleItemSink.getName());
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleEnchantmentSinkMK2.getName(), ModuleEnchantmentSink.getName());
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleCreativeTabBasedItemSink.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleCrafter.getName(), null);
        registerModuleRecipe(LogisticsProgramCompilerBlockEntity.ProgramCategories.CHASSIS_2,
            ModuleActiveSupplier.getName(), null);
    }

}
