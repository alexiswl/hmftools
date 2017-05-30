package com.hartwig.hmftools.common.purple.region;

import static java.util.stream.Collectors.toList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.purple.FittedRegion;
import com.hartwig.hmftools.common.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

public enum ConsolidatedRegionFactory {
    ;

    @NotNull
    public static List<ConsolidatedRegion> highConfidence(@NotNull final List<FittedRegion> fittedRegions) {
        return new HighConfidenceRegions().highConfidence(fittedRegions);
    }

    @NotNull
    public static List<ConsolidatedRegion> smooth(@NotNull final List<FittedRegion> fittedRegions,
            @NotNull final List<ConsolidatedRegion> broadRegions) {
        final List<ConsolidatedRegion> result = Lists.newArrayList();

        final Set<String> orderedChromosomes = broadRegions.stream().map(GenomeRegion::chromosome).collect(
                Collectors.toCollection(LinkedHashSet::new));

        for (final String orderedChromosome : orderedChromosomes) {
            final List<FittedRegion> chromosomeCopyNumbers = fittedRegions.stream().filter(
                    matchesChromosome(orderedChromosome)).collect(toList());

            final List<ConsolidatedRegion> chromosomeBroadRegions = broadRegions.stream().filter(
                    matchesChromosome(orderedChromosome)).collect(toList());

            final List<ConsolidatedRegion> smoothRegions = new SmoothedRegions(chromosomeBroadRegions,
                    chromosomeCopyNumbers).getSmoothedRegions();

            result.addAll(RegionStepFilter.filter(smoothRegions));
        }

        return result;
    }

    @NotNull
    private static <T extends GenomeRegion> Predicate<T> matchesChromosome(@NotNull final String chromosome) {
        return t -> t.chromosome().equals(chromosome);
    }
}
