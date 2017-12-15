package com.hartwig.hmftools.common.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SomaticVariantFactoryTest {

    private static final double EPSILON = 1.0e-10;

    @Test
    public void canReadSampleNameFromHeader() {
        final String header = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tsample";
        assertEquals("sample", SomaticVariantFactoryOld.sampleFromHeaderLine(header));
    }

    @Test
    public void canReadAndWriteCorrectSomaticVariant() {
        final String part1 = "15 \t 12345678 \t rs1;UCSC \t C \t A,G \t <qual> \t";
        final String part2 = "<filter>";
        final String part3 = "\t set=varscan-freebayes; \t <format> \t 0/1:60,60:121";
        final String line = part1 + part2 + part3;

        final SomaticVariant variant = SomaticVariantFactoryOld.fromVCFLine(line);
        assertEquals("15", variant.chromosome());
        assertEquals(12345678, variant.position());
        assertEquals(VariantType.SNP, variant.type());
        assertTrue(variant.isDBSNP());
        assertFalse(variant.isCOSMIC());

        assertEquals("C", variant.ref());
        assertEquals("A,G", variant.alt());

        assertEquals(2, variant.callerCount());
        assertTrue(variant.callers().contains(SomaticVariantConstants.FREEBAYES));
        assertTrue(variant.callers().contains(SomaticVariantConstants.VARSCAN));
        assertFalse(variant.callers().contains(SomaticVariantConstants.STRELKA));
        assertFalse(variant.callers().contains(SomaticVariantConstants.MUTECT));

        assertEquals(0.5, variant.alleleFrequency(), EPSILON);
        assertEquals(120, variant.totalReadCount(), EPSILON);

        final String filter = "KODU_FILTER";
    }

    @Test
    public void incorrectSampleFieldYieldsMissingReadCounts() {
        final String missingAFLine = "0 \t 1 \t 2 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 9";
        final SomaticVariant missingAFVariant = SomaticVariantFactoryOld.fromVCFLine(missingAFLine);
        assertEquals(Double.NaN, missingAFVariant.alleleFrequency(), EPSILON);
        assertEquals(0, missingAFVariant.totalReadCount(), EPSILON);

        final String missingRefCovLine = "0 \t 1 \t 2 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 0/1:60:113";
        final SomaticVariant missingRefCovVariant = SomaticVariantFactoryOld.fromVCFLine(missingRefCovLine);
        assertEquals(Double.NaN, missingRefCovVariant.alleleFrequency(), EPSILON);
        assertEquals(0, missingRefCovVariant.totalReadCount(), EPSILON);
    }

    @Test
    public void recognizeFilterInVarscan() {
        final String line = "0 \t 1 \t 2 \t 3 \t 4 \t 5 \t 6 \t set=freebayes-filterInVarscan; \t 8 \t 9";
        final SomaticVariant variant = SomaticVariantFactoryOld.fromVCFLine(line);

        assertEquals(1, variant.callerCount());
        assertFalse(variant.callers().contains(SomaticVariantConstants.VARSCAN));
    }

    @Test
    public void correctDBSNPAndCOSMIC() {
        final String both = "0 \t 1 \t rs1;COSM2 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 9";

        final SomaticVariant hasBoth = SomaticVariantFactoryOld.fromVCFLine(both);
        assertTrue(hasBoth.isDBSNP());
        assertTrue(hasBoth.isCOSMIC());
        assertEquals("COSM2", hasBoth.cosmicID());

        final String dbsnpOnly = "0 \t 1 \t rs1 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 9";
        final SomaticVariant hasDBSNPOnly = SomaticVariantFactoryOld.fromVCFLine(dbsnpOnly);
        assertTrue(hasDBSNPOnly.isDBSNP());
        assertFalse(hasDBSNPOnly.isCOSMIC());

        final String cosmicOnly = "0 \t 1 \t COSM2 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 9";
        final SomaticVariant hasCOSMICOnly = SomaticVariantFactoryOld.fromVCFLine(cosmicOnly);
        assertFalse(hasCOSMICOnly.isDBSNP());
        assertTrue(hasCOSMICOnly.isCOSMIC());
        assertEquals("COSM2", hasCOSMICOnly.cosmicID());

        final String none = "0 \t 1 \t 2 \t 3 \t 4 \t 5 \t 6 \t 7 \t 8 \t 9";
        final SomaticVariant hasNone = SomaticVariantFactoryOld.fromVCFLine(none);
        assertFalse(hasNone.isDBSNP());
        assertFalse(hasNone.isCOSMIC());
    }

}