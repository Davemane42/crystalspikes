package com.crystalspikes.worldgen;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.DripstoneUtils;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Optional;

public class CrystalSpikeFeature extends Feature<CrystalSpikeConfig> {

    public CrystalSpikeFeature(Codec<CrystalSpikeConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CrystalSpikeConfig> context) {
        WorldGenLevel world = context.level();
        BlockPos blockPos = context.origin();
        RandomSource random = context.random();
        CrystalSpikeConfig config = context.config();
        HashSet<BlockPos> trigList = Sets.newHashSet();
        HashSet<BlockPos> clusterPos = Sets.newHashSet();
        int startRadius = config.baseRadius().sample(random) + 1;
        Vector3f direction = config.sampleDirection(random);
        float slopeAngle = config.slopeAngle().sample(random);
        double taper = Math.tan(slopeAngle * Mth.DEG_TO_RAD);
        int height = config.height().map(h -> h.sample(random))
                .orElseGet(() -> taper > 1.0E-4 ? Mth.ceil(startRadius / taper) + 1 : startRadius * 2 + 8);

        BlockPos probe = BlockPos.containing(
                blockPos.getX() + 0.5 + direction.x,
                blockPos.getY() + 0.5 + direction.y,
                blockPos.getZ() + 0.5 + direction.z);
        if (probe.equals(blockPos)
                || !world.getBlockState(blockPos).is(config.anchor_tag())
                || !world.isStateAtPosition(probe, DripstoneUtils::isEmptyOrWaterOrLava)) {
            return false;
        }
        if (!this.placeSpike(world, blockPos, startRadius, direction, taper, height, trigList, random, config)) {
            return false;
        }
        return placeCrystals(world, random, config, trigList, clusterPos);
    }

    private boolean placeCrystals(WorldGenLevel world, RandomSource random, CrystalSpikeConfig config, HashSet<BlockPos> trigList, HashSet<BlockPos> clusterPos) {
        boolean flag = false;
        // Core:
        for (BlockPos pos : trigList) {
            if (world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWaterOrLava)) {
                this.setBlock(world, pos, config.core().getState(random, pos));
                clusterPos.add(pos);
                flag = true;
            }
        }
        // Outer layer: only blocks exposed to space outside the spike itself
        HashSet<BlockPos> shellPos = Sets.newHashSet();
        for (BlockPos pos : clusterPos) {
            for (Direction direction : Direction.values()) {
                BlockPos relative = pos.relative(direction);
                if (!clusterPos.contains(relative) && world.isStateAtPosition(relative, DripstoneUtils::isEmptyOrWaterOrLava)) {
                    this.setBlock(world, pos, config.outer_layer().getState(random, pos));
                    shellPos.add(pos);
                    break;
                }
            }
        }
        
        // Decorators roll per shell block; horizontal faces use the wall_decorator when present
        for (BlockPos pos : shellPos) {
            if (config.surface_decorator().isPresent() && random.nextFloat() < config.decoratorChance()) {
                for (Direction direction : Direction.values()) {
                    Optional<BlockStateProvider> provider = direction.getAxis().isHorizontal()
                            ? config.wall_decorator().or(() -> config.surface_decorator())
                            : config.surface_decorator();
                    if (provider.isEmpty()) {
                        continue;
                    }
                    BlockPos relative = pos.relative(direction);
                    if (random.nextFloat() < config.decoratorFaceChance() && world.isStateAtPosition(relative, DripstoneUtils::isEmptyOrWater)) {
                        BlockState blockState = provider.get().getState(random, relative);
                        if (blockState.hasProperty(BlockStateProperties.FACING)) {
                            blockState = blockState.setValue(BlockStateProperties.FACING, direction);
                        }
                        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                            blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, world.getFluidState(relative).getType() == Fluids.WATER);
                        }
                        if (blockState.canSurvive(world, relative)) {
                            this.setBlock(world, relative, blockState);
                        }
                    }
                }
            }
        }
        return flag;
    }

    // Scans the bounding box and keep voxels that are inside the cone
    public boolean placeSpike(LevelAccessor world, BlockPos origin, int startRadius, Vector3f axis,
                              double taper, int height, HashSet<BlockPos> crystalPos,
                              RandomSource random, CrystalSpikeConfig config) {
        if (startRadius < 1) {
            return false;
        }
        boolean flag = false;
        HashSet<BlockPos> baseBand = Sets.newHashSet();
        double reach = startRadius + 1;
        int minX = Mth.floor(origin.getX() + Math.min(0, axis.x() * height) - reach);
        int maxX = Mth.ceil(origin.getX() + Math.max(0, axis.x() * height) + reach);
        int minY = Mth.floor(origin.getY() + Math.min(0, axis.y() * height) - reach);
        int maxY = Mth.ceil(origin.getY() + Math.max(0, axis.y() * height) + reach);
        int minZ = Mth.floor(origin.getZ() + Math.min(0, axis.z() * height) - reach);
        int maxZ = Mth.ceil(origin.getZ() + Math.max(0, axis.z() * height) + reach);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            double dx = x - origin.getX();
            for (int y = minY; y <= maxY; y++) {
                double dy = y - origin.getY();
                for (int z = minZ; z <= maxZ; z++) {
                    double dz = z - origin.getZ();
                    double s = dx * axis.x() + dy * axis.y() + dz * axis.z();
                    if (s < 0 || s >= height) {
                        continue;
                    }
                    double radius = startRadius - s * taper;
                    if (radius <= 0) {
                        continue;
                    }
                    double radialSq = dx * dx + dy * dy + dz * dz - s * s;
                    if (radialSq > radius * radius) {
                        continue;
                    }
                    pos.set(x, y, z);
                    if (s < 1) {
                        baseBand.add(pos.immutable());
                    }
                    if (world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWaterOrLava)) {
                        crystalPos.add(pos.immutable());
                        flag = true;
                    } else {
                        crystalPos.remove(pos);
                    }
                }
            }
        }

        // Roots: grow from baseBand to rootDepth
        int rootDepth = config.rootDepth();
        if (rootDepth > 0) {
            HashSet<BlockPos> rootPos = Sets.newHashSet();
            Direction rootDirection = Direction.getNearest(-axis.x(), -axis.y(), -axis.z());
            BlockPos.MutableBlockPos root = new BlockPos.MutableBlockPos();
            for (BlockPos base : baseBand) {
                root.set(base);
                for (int d = 0; d < rootDepth; d++) {
                    root.move(rootDirection);
                    if (!world.isStateAtPosition(root, DripstoneUtils::isEmptyOrWaterOrLava)) {
                        break;
                    }
                    rootPos.add(root.immutable());
                }
            }
            crystalPos.addAll(rootPos);
            flag |= !rootPos.isEmpty();
            baseBand.addAll(rootPos);
        }

        // Base: change blocks around root/baseBand
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (BlockPos base : baseBand) {
            for (Direction direction : Direction.values()) {
                neighbor.setWithOffset(base, direction);
                if (world.getBlockState(neighbor).is(config.anchor_tag())) {
                    for (Direction face : Direction.values()) {
                        if (world.isStateAtPosition(neighbor.relative(face), DripstoneUtils::isEmptyOrWaterOrLava)) {
                            world.setBlock(neighbor, config.baseOrDefault().getState(random, neighbor), 2);
                            break;
                        }
                    }
                }
            }
        }
        return flag;
    }
}
