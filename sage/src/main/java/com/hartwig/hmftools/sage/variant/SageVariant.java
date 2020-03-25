package com.hartwig.hmftools.sage.variant;

import java.util.List;
import java.util.Set;

import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.variant.hotspot.VariantHotspot;
import com.hartwig.hmftools.sage.read.ReadContext;
import com.hartwig.hmftools.sage.read.ReadContextCounter;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SageVariant implements GenomePosition {

    private final Set<String> filters;
    private final SageVariantTier tier;
    private final VariantHotspot variant;
    private final List<ReadContextCounter> normalAltContexts;
    private final List<ReadContextCounter> tumorAltContexts;

    private int localPhaseSet;

    public SageVariant(@NotNull final SageVariantTier tier, @NotNull final VariantHotspot variant, @NotNull final Set<String> filters,
            final List<ReadContextCounter> normal, final List<ReadContextCounter> tumorAltContexts) {
        assert (!tumorAltContexts.isEmpty());
        this.tier = tier;
        this.normalAltContexts = normal;
        this.tumorAltContexts = tumorAltContexts;
        this.filters = filters;
        this.variant = variant;
    }

    @NotNull
    public String ref() {
        return variant.alt();
    }

    @NotNull
    public String alt() {
        return variant.alt();
    }

    public long end() {
        return position() + ref().length() - 1;
    }

    public boolean isIndel() {
        return variant.ref().length() != variant.alt().length();
    }

    public boolean isInsert() {
        return variant.ref().length() < variant.alt().length();
    }

    public boolean isDelete() {
        return variant.ref().length() > variant.alt().length();
    }

    public int localPhaseSet() {
        return localPhaseSet;
    }

    public void localPhaseSet(int localPhaseSet) {
        this.localPhaseSet = localPhaseSet;
    }

    public boolean isPassing() {
        return filters.isEmpty();
    }

    public boolean isTumorEmpty() {
        return tumorAltContexts.isEmpty();
    }

    public boolean isNormalEmpty() {
        return normalAltContexts.isEmpty();
    }

    @NotNull
    public VariantHotspot variant() {
        return variant;
    }

    @NotNull
    public SageVariantTier tier() {
        return tier;
    }

    @NotNull
    public Set<String> filters() {
        return filters;
    }

    @NotNull
    public ReadContextCounter primaryNormal() {
        return normalAltContexts.get(0);
    }

    @NotNull
    public ReadContext readContext() {
        return tumorAltContexts.isEmpty() ? normalAltContexts.get(0).readContext() : tumorAltContexts.get(0).readContext();
    }

    @NotNull
    public List<ReadContextCounter> normalAltContexts() {
        return normalAltContexts;
    }

    @NotNull
    public List<ReadContextCounter> tumorAltContexts() {
        return tumorAltContexts;
    }

    @NotNull
    @Override
    public String chromosome() {
        return variant().chromosome();
    }

    @Override
    public long position() {
        return variant().position();
    }

    public int totalQuality() {
        return tumorAltContexts.stream().mapToInt(ReadContextCounter::tumorQuality).sum();
    }

    public int maxQuality() {
        return tumorAltContexts.stream().mapToInt(ReadContextCounter::tumorQuality).max().orElse(0);
    }

}
