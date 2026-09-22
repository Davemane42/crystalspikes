package com.crystalspikes.worldgen;

import com.crystalspikes.CrystalSpikes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrystalSpikesPlacements {

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, CrystalSpikes.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<UnderIslandPlacement>> UNDER_ISLAND =
            PLACEMENT_MODIFIERS.register("under_island", () -> () -> UnderIslandPlacement.CODEC);
}
