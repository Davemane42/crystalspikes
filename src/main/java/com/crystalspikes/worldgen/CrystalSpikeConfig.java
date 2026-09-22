package com.crystalspikes.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import java.util.Optional;

public record CrystalSpikeConfig(BlockStateProvider core, BlockStateProvider outer_layer,
                                 Optional<BlockStateProvider> surface_decorator,
                                 Optional<BlockStateProvider> base,
                                 IntProvider baseRadius, CaveSurface crystal_direction,
                                 TagKey<Block> anchor_tag) implements FeatureConfiguration {

    public static final Codec<CrystalSpikeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("core").forGetter(CrystalSpikeConfig::core),
            BlockStateProvider.CODEC.fieldOf("outer_layer").forGetter(CrystalSpikeConfig::outer_layer),
            BlockStateProvider.CODEC.optionalFieldOf("surface_decorator").forGetter(CrystalSpikeConfig::surface_decorator),
            BlockStateProvider.CODEC.optionalFieldOf("base").forGetter(CrystalSpikeConfig::base),
            IntProvider.codec(1, 32).fieldOf("base_radius").forGetter(CrystalSpikeConfig::baseRadius),
            CaveSurface.CODEC.fieldOf("crystal_direction").forGetter(CrystalSpikeConfig::crystal_direction),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("anchor_tag", BlockTags.BASE_STONE_OVERWORLD).forGetter(CrystalSpikeConfig::anchor_tag)
    ).apply(instance, CrystalSpikeConfig::new));

    public BlockStateProvider baseOrDefault() {
        return base.orElse(BlockStateProvider.simple(Blocks.CALCITE));
    }
}
