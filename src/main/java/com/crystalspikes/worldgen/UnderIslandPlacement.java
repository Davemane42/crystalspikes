package com.crystalspikes.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.DripstoneUtils;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// Finds anchor blocks on the underside of terrain: blocks in anchor_tag with air/water/lava below.
public class UnderIslandPlacement extends PlacementModifier {

    public static final MapCodec<UnderIslandPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.codec(Registries.BLOCK).optionalFieldOf("anchor_tag", BlockTags.BASE_STONE_OVERWORLD).forGetter(UnderIslandPlacement::anchorTag),
            Codec.INT.optionalFieldOf("min_y").forGetter(p -> p.minY),
            Codec.INT.optionalFieldOf("max_y").forGetter(p -> p.maxY),
            Selection.CODEC.optionalFieldOf("selection", Selection.RANDOM).forGetter(p -> p.selection)
    ).apply(instance, UnderIslandPlacement::new));

    private final TagKey<Block> anchorTag;
    private final Optional<Integer> minY;
    private final Optional<Integer> maxY;
    private final Selection selection;

    public UnderIslandPlacement(TagKey<Block> anchorTag, Optional<Integer> minY, Optional<Integer> maxY, Selection selection) {
        this.anchorTag = anchorTag;
        this.minY = minY;
        this.maxY = maxY;
        this.selection = selection;
    }

    public TagKey<Block> anchorTag() {
        return this.anchorTag;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        WorldGenLevel level = context.getLevel();
        int minY = Math.max(this.minY.orElse(level.getMinBuildHeight()), level.getMinBuildHeight() + 1);
        int maxY = Math.min(this.maxY.orElse(level.getMaxBuildHeight() - 1), level.getMaxBuildHeight() - 1);

        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos(pos.getX(), minY, pos.getZ());
        for (int y = minY; y <= maxY; y++) {
            mut.setY(y);
            if (level.getBlockState(mut).is(this.anchorTag)
                    && DripstoneUtils.isEmptyOrWaterOrLava(level.getBlockState(mut.below()))) {
                candidates.add(mut.immutable());
            }
        }

        if (candidates.isEmpty()) {
            return Stream.empty();
        }
        BlockPos chosen = switch (this.selection) {
            case LOWEST -> candidates.getFirst();
            case HIGHEST -> candidates.getLast();
            case RANDOM -> candidates.get(random.nextInt(candidates.size()));
        };
        return Stream.of(chosen);
    }

    @Override
    public PlacementModifierType<?> type() {
        return CrystalSpikesPlacements.UNDER_ISLAND.get();
    }

    public enum Selection implements StringRepresentable {
        RANDOM("random"),
        LOWEST("lowest"),
        HIGHEST("highest");

        public static final Codec<Selection> CODEC = StringRepresentable.fromEnum(Selection::values);

        private final String name;

        Selection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
