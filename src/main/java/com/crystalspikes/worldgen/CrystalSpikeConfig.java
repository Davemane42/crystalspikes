package com.crystalspikes.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public record CrystalSpikeConfig(BlockStateProvider core, BlockStateProvider outer_layer,
                                 Optional<BlockStateProvider> surface_decorator,
                                 Optional<BlockStateProvider> wall_decorator,
                                 Optional<BlockStateProvider> base,
                                 IntProvider baseRadius, List<Direction> direction,
                                 float minAngle, float maxAngle,
                                 FloatProvider slopeAngle, Optional<IntProvider> height,
                                 float decoratorChance, float decoratorFaceChance,
                                 int rootDepth,
                                 TagKey<Block> anchor_tag) implements FeatureConfiguration {

    // arctan(1/2), the taper of the original hard-coded 1:2 cone
    public static final float DEFAULT_SLOPE_DEG = 26.565f;

    public static final Codec<CrystalSpikeConfig> CODEC = RecordCodecBuilder.<CrystalSpikeConfig>create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("core").forGetter(CrystalSpikeConfig::core),
            BlockStateProvider.CODEC.fieldOf("outer_layer").forGetter(CrystalSpikeConfig::outer_layer),
            BlockStateProvider.CODEC.optionalFieldOf("surface_decorator").forGetter(CrystalSpikeConfig::surface_decorator),
            BlockStateProvider.CODEC.optionalFieldOf("wall_decorator").forGetter(CrystalSpikeConfig::wall_decorator),
            BlockStateProvider.CODEC.optionalFieldOf("base").forGetter(CrystalSpikeConfig::base),
            IntProvider.codec(1, 32).fieldOf("base_radius").forGetter(CrystalSpikeConfig::baseRadius),
            Direction.CODEC.listOf().validate(directions -> directions.isEmpty()
                            ? DataResult.error(() -> "direction must contain at least one entry")
                            : DataResult.success(directions))
                    .fieldOf("direction").forGetter(CrystalSpikeConfig::direction),
            Codec.floatRange(0, 180).optionalFieldOf("min_angle", 0f).forGetter(CrystalSpikeConfig::minAngle),
            Codec.floatRange(0, 180).optionalFieldOf("max_angle", 0f).forGetter(CrystalSpikeConfig::maxAngle),
            FloatProvider.codec(0, 85).optionalFieldOf("slope_angle", ConstantFloat.of(DEFAULT_SLOPE_DEG)).forGetter(CrystalSpikeConfig::slopeAngle),
            IntProvider.codec(1, 256).optionalFieldOf("height").forGetter(CrystalSpikeConfig::height),
            Codec.floatRange(0, 1).optionalFieldOf("decorator_chance", 1f / 6f).forGetter(CrystalSpikeConfig::decoratorChance),
            Codec.floatRange(0, 1).optionalFieldOf("decorator_face_chance", 0.5f).forGetter(CrystalSpikeConfig::decoratorFaceChance),
            Codec.intRange(0, 64).optionalFieldOf("root_depth", 4).forGetter(CrystalSpikeConfig::rootDepth),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("anchor_tag", BlockTags.BASE_STONE_OVERWORLD).forGetter(CrystalSpikeConfig::anchor_tag)
    ).apply(instance, CrystalSpikeConfig::new)).validate(config -> config.minAngle <= config.maxAngle
            ? DataResult.success(config)
            : DataResult.error(() -> "min_angle must be <= max_angle"));

    // Picks a random unit vector within the cone band around one of the main directions.
    public Vector3f sampleDirection(RandomSource random) {
        Direction main = this.direction.get(random.nextInt(this.direction.size()));
        Vector3f axis = new Vector3f(main.getStepX(), main.getStepY(), main.getStepZ());

        Vector3f ref = Math.abs(axis.x) < 0.99f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = axis.cross(ref, new Vector3f()).normalize();
        Vector3f v = axis.cross(u, new Vector3f());

        float phi = random.nextFloat() * Mth.TWO_PI;
        // Uniform in cos(theta) so wide spreads don't cluster around the main axis
        float cosTheta = Mth.lerp(random.nextFloat(),
                Mth.cos(this.minAngle * Mth.DEG_TO_RAD),
                Mth.cos(this.maxAngle * Mth.DEG_TO_RAD));
        float sinTheta = Mth.sqrt(1 - cosTheta * cosTheta);

        Vector3f tilt = u.mul(Mth.cos(phi), new Vector3f())
                .add(v.mul(Mth.sin(phi), new Vector3f()))
                .mul(sinTheta);
        return axis.mul(cosTheta, new Vector3f()).add(tilt).normalize();
    }

    public BlockStateProvider baseOrDefault() {
        return base.orElse(BlockStateProvider.simple(Blocks.CALCITE));
    }
}
