package com.hartwig.hmftools.common.purple.segment;

import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(passAnnotations = { NotNull.class, Nullable.class })
abstract class SVSegment implements GenomePosition {
    public abstract StructuralVariantType type();
}
