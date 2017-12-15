package com.hartwig.hmftools.common.variant;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.VariantContext;

final class VariantAnnotationFactory {

    private static final Logger LOGGER = LogManager.getLogger(VariantAnnotationFactory.class);

    private static final String ANNOTATIONS_IDENTIFIER = "ANN";
    private static final String START_IDENTIFIER = "ANN=";
    private static final String END_IDENTIFIER = ";";
    private static final String ANNOTATION_SEPARATOR = ",";
    private static final String FIELD_SEPARATOR = "\\|";
    private static final String CONSEQUENCE_SEPARATOR = "&";

    private static final int EXPECTED_FIELD_SIZE_PER_ANNOTATION = 16;

    private VariantAnnotationFactory() {
    }

    @NotNull
    @Deprecated
    static List<VariantAnnotation> fromVCFInfoField(@NotNull final String info) {
        final List<VariantAnnotation> annotations = Lists.newArrayList();
        final int startIndex = info.indexOf(START_IDENTIFIER);
        if (startIndex >= 0) {
            String fullAnnotationString = info.substring(startIndex + START_IDENTIFIER.length());
            final int endIndex = fullAnnotationString.indexOf(END_IDENTIFIER);
            if (endIndex >= 0) {
                fullAnnotationString = fullAnnotationString.substring(0, endIndex);
            }
            for (final String annotationString : fullAnnotationString.split(ANNOTATION_SEPARATOR)) {
                final String[] parts = enforceMinLength(annotationString.split(FIELD_SEPARATOR), EXPECTED_FIELD_SIZE_PER_ANNOTATION);
                if (parts.length == EXPECTED_FIELD_SIZE_PER_ANNOTATION) {
                    annotations.add(fromParts(parts));
                } else {
                    LOGGER.warn("Annotation found with invalid field count: " + annotationString);
                }
            }
        }
        return annotations;
    }

    @NotNull
    static List<VariantAnnotation> fromContext(@NotNull final VariantContext context) {
        if (context.hasAttribute(ANNOTATIONS_IDENTIFIER)) {
            return context.getAttributeAsStringList(ANNOTATIONS_IDENTIFIER, "")
                    .stream()
                    .map(x -> enforceMinLength(x.trim().split(FIELD_SEPARATOR), EXPECTED_FIELD_SIZE_PER_ANNOTATION))
                    .filter(VariantAnnotationFactory::isCorrectNumberOfParts)
                    .map(VariantAnnotationFactory::fromParts)
                    .collect(Collectors.toList());

        }
        return Collections.emptyList();
    }

    private static boolean isCorrectNumberOfParts(@NotNull String[] parts) {
        if (parts.length == EXPECTED_FIELD_SIZE_PER_ANNOTATION) {
            return true;
        }

        final StringJoiner joiner = new StringJoiner("|");
        Stream.of(parts).forEach(joiner::add);

        LOGGER.warn("Annotation found with invalid field count: " + joiner.toString());
        return false;
    }

    @NotNull
    private static VariantAnnotation fromParts(@NotNull final String[] parts) {
        return ImmutableVariantAnnotation.builder()
                .allele(parts[0])
                .consequences(toConsequences(parts[1]))
                .severity(parts[2])
                .gene(parts[3])
                .geneID(parts[4])
                .featureType(parts[5])
                .featureID(parts[6])
                .transcriptBioType(parts[7])
                .rank(parts[8])
                .hgvsCoding(parts[9])
                .hgvsProtein(parts[10])
                .cDNAPosAndLength(parts[11])
                .cdsPosAndLength(parts[12])
                .aaPosAndLength(parts[13])
                .distance(parts[14])
                .addition(parts[15])
                .build();
    }

    @NotNull
    private static String[] enforceMinLength(@NotNull String[] parts, int minSize) {
        if (parts.length > minSize) {
            return parts;
        } else {
            final String[] values = new String[minSize];
            for (int i = 0; i < minSize; i++) {
                values[i] = i < parts.length ? parts[i] : Strings.EMPTY;
            }
            System.arraycopy(parts, 0, values, 0, parts.length);

            return values;
        }
    }

    @NotNull
    private static List<VariantConsequence> toConsequences(@NotNull final String consequenceString) {
        final List<VariantConsequence> consequences = Lists.newArrayList();
        final String[] parts = consequenceString.split(CONSEQUENCE_SEPARATOR);
        for (final String part : parts) {
            boolean found = false;
            for (final VariantConsequence consequence : VariantConsequence.values()) {
                if (consequence.isParentTypeOf(part)) {
                    found = true;
                    consequences.add(consequence);
                }
            }
            if (!found) {
                LOGGER.warn("Could not resolve consequence: " + part);
                consequences.add(VariantConsequence.OTHER);
            }
        }
        return consequences;
    }
}
