package com.crystalspikes;

import com.crystalspikes.worldgen.CrystalSpikeConfig;
import com.crystalspikes.worldgen.CrystalSpikeFeature;
import com.crystalspikes.worldgen.UnderIslandPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CrystalSpikes.MOD_ID)
public class CrystalSpikes {

    public static final String MOD_ID = "crystalspikes";

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, MOD_ID);

    public static final DeferredHolder<Feature<?>, CrystalSpikeFeature> CRYSTAL_SPIKE =
            FEATURES.register("crystal_spike", () -> new CrystalSpikeFeature(CrystalSpikeConfig.CODEC));
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<UnderIslandPlacement>> UNDER_ISLAND =
            PLACEMENT_MODIFIERS.register("under_island", () -> () -> UnderIslandPlacement.CODEC);

    public CrystalSpikes(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        PLACEMENT_MODIFIERS.register(modEventBus);
    }
}
