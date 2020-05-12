package com.hartwig.hmftools.gripss

import com.google.common.collect.Sets
import htsjdk.variant.variantcontext.GenotypeBuilder
import htsjdk.variant.variantcontext.VariantContext
import htsjdk.variant.variantcontext.VariantContextBuilder
import htsjdk.variant.vcf.VCFCodec
import htsjdk.variant.vcf.VCFHeader
import htsjdk.variant.vcf.VCFHeaderVersion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuralVariantContextTest {

    private val codec: VCFCodec by lazy {
        val codec = VCFCodec()
        val header = VCFHeader(Sets.newHashSet(), Sets.newHashSet("NORMAL", "TUMOR"))
        codec.setVCFHeader(header, VCFHeaderVersion.VCF4_2)
        return@lazy codec
    }

    @Test
    fun testBreakendAssemblyReadPair() {
        val single = createSingle();
        assertTrue(single.toSv().breakendAssemblyReadPairsFilter())
        assertFalse(single.setAttribute("BASRP", 1).toSv().breakendAssemblyReadPairsFilter())

        val shortDel = shortDel();
        assertFalse(shortDel.toSv().breakendAssemblyReadPairsFilter())
        assertFalse(shortDel.setAttribute("BASRP", 1).toSv().breakendAssemblyReadPairsFilter())
    }

    @Test
    fun testImpreciseFilter() {
        val victim = createSingle()
        assertFalse(victim.toSv().impreciseFilter())
        assertFalse(victim.setAttribute("IMPRECISE", false).toSv().impreciseFilter())
        assertTrue(victim.setAttribute("IMPRECISE", true).toSv().impreciseFilter())
    }

    @Test
    fun testNormalSupportFilter() {
        assertTrue(createSingle().fragmentSupport(3, 9).toSv().normalSupportFilter(0.03))
        assertFalse(createSingle().fragmentSupport(3, 100).toSv().normalSupportFilter(0.03))
        assertTrue(createSingle().fragmentSupport(3, 100).toSv().normalSupportFilter(0.0299))
    }

    @Test
    fun testQualFilter() {
        val breakEnd = createSingle().qual(200).toSv();
        assertTrue(breakEnd.qualFilter(201, 1000))
        assertFalse(breakEnd.qualFilter(200, 1000))
        assertFalse(breakEnd.qualFilter(199, 1000))

        val breakPoint = createBreakPoint().qual(200).toSv();
        assertTrue(breakPoint.qualFilter(1000, 201))
        assertFalse(breakPoint.qualFilter(1000, 200))
        assertFalse(breakPoint.qualFilter(1000, 199))
    }

    @Test
    fun testPolyGFilters() {
        assertFalse(createVariant(100, "A", "A" + "G".repeat(100) + "[1:123[").toSv().polyGCFilter())

        assertTrue(createVariant(100, "A", "A" + "G".repeat(16) + ".").toSv().polyGCFilter())
        assertTrue(createVariant(100, "A", "A" + "C".repeat(16) + ".").toSv().polyGCFilter())
        assertFalse(createVariant(100, "A", "A" + "G".repeat(15) + ".").toSv().polyGCFilter())
        assertFalse(createVariant(100, "A", "A" + "C".repeat(15) + ".").toSv().polyGCFilter())
        assertFalse(createVariant(100, "A", "A" + "A".repeat(16) + ".").toSv().polyGCFilter())
        assertFalse(createVariant(100, "A", "A" + "T".repeat(16) + ".").toSv().polyGCFilter())
    }

    @Test
    fun testHomologyLengthFilter() {
        val attribute = "HOMLEN"
        val single = createSingle().setAttribute(attribute, 1000).toSv()
        assertFalse(single.homologyLengthFilter(1))

        val dup = shortDup().setAttribute(attribute, 50).toSv()
        assertFalse(dup.homologyLengthFilter(50))
        assertTrue(dup.homologyLengthFilter(49))
        assertFalse(dup.homologyLengthFilterShortInversion(50))
        assertFalse(dup.homologyLengthFilterShortInversion(49))

        val inv = shortInv().setAttribute(attribute, 50).toSv()
        assertFalse(inv.homologyLengthFilter(50))
        assertTrue(inv.homologyLengthFilter(49))
        assertFalse(inv.homologyLengthFilterShortInversion(50))
        assertTrue(inv.homologyLengthFilterShortInversion(49))
    }

    @Test
    fun testInexactHomologyLengthFilter() {
        val attribute = "IHOMPOS"
        assertFalse(createSingle().setAttribute(attribute, listOf(1, 51)).toSv().inexactHomologyLengthFilter(1))

        val bnd = bnd().setAttribute(attribute, listOf(1, 51)).toSv()
        assertFalse(bnd.inexactHomologyLengthFilter(50))
        assertTrue(bnd.inexactHomologyLengthFilter(49))
        assertFalse(bnd.inexactHomologyLengthShortDelFilter(50))
        assertFalse(bnd.inexactHomologyLengthShortDelFilter(49))

        val tinyDel = shortDel(100, 110).setAttribute(attribute, listOf(1, 51)).toSv()
        assertFalse(tinyDel.inexactHomologyLengthFilter(50))
        assertTrue(tinyDel.inexactHomologyLengthFilter(49))
        assertFalse(tinyDel.inexactHomologyLengthShortDelFilter(50))
        assertFalse(tinyDel.inexactHomologyLengthShortDelFilter(49))

        val shortDel = shortDel(100, 310).setAttribute(attribute, listOf(1, 51)).toSv()
        assertFalse(shortDel.inexactHomologyLengthFilter(50))
        assertTrue(shortDel.inexactHomologyLengthFilter(49))
        assertFalse(shortDel.inexactHomologyLengthShortDelFilter(50))
        assertTrue(shortDel.inexactHomologyLengthShortDelFilter(49))

        val shortDup = shortDup().setAttribute(attribute, listOf(1, 51)).toSv()
        assertFalse(shortDup.inexactHomologyLengthFilter(1))
    }

    @Test
    fun testLongDPSupport() {
        assertFalse(shortDel().toSv().longDPSupportFilter())
        assertTrue(bnd().toSv().longDPSupportFilter())
        assertFalse(bnd().addGenotypeAttribute("RP", 1, 0).toSv().longDPSupportFilter())
        assertFalse(bnd().addGenotypeAttribute("RP", 0, 1).toSv().longDPSupportFilter())
        assertFalse(bnd().addGenotypeAttribute("ASRP", 1, 0).toSv().longDPSupportFilter())
        assertFalse(bnd().addGenotypeAttribute("ASRP", 0, 1).toSv().longDPSupportFilter())
    }

    @Test
    fun testShortSRSupportFilter() {
        val bnd = bnd().splitReads(1, 0).toSv()
        assertFalse(bnd.shortSplitReadTumorFilter())

        assertFalse(shortDel().splitReads(0, 1).toSv().shortSplitReadTumorFilter())
        assertTrue(shortDel().splitReads(0, 0).toSv().shortSplitReadTumorFilter())
    }

    @Test
    fun testShortSRNormalFilter() {
        val bnd = bnd().splitReads(1, 0).toSv()
        assertFalse(bnd.shortSplitReadNormalFilter())

        assertFalse(shortDel().splitReads(0, 1).toSv().shortSplitReadNormalFilter())
        assertTrue(shortDel().splitReads(1, 1).toSv().shortSplitReadNormalFilter())
    }


    @Test
    fun testVariantCreationFunctions() {
        val shortDup = shortDup().toSv()
        assertFalse(shortDup.isSingle)
        assertTrue(shortDup.isShort)
        assertTrue(shortDup.isShortDup)
        assertFalse(shortDup.isShortDel)
        assertFalse(shortDup.isShortIns)

        val shortDel = shortDel().toSv()
        assertFalse(shortDup.isSingle)
        assertTrue(shortDel.isShort)
        assertFalse(shortDel.isShortDup)
        assertTrue(shortDel.isShortDel)
        assertFalse(shortDel.isShortIns)

        val bnd = bnd().toSv()
        assertFalse(bnd.isSingle)
        assertFalse(bnd.isShort)
        assertFalse(bnd.isShortDup)
        assertFalse(bnd.isShortDel)
        assertFalse(bnd.isShortIns)

        val shortInv = shortInv().toSv()
        assertTrue(shortInv.variantType is Inversion)

        assertTrue(createSingle().toSv().isSingle)
        assertFalse(createBreakPoint().toSv().isSingle)
    }

    private fun bnd(): VariantContext = createVariant(80, "A", "ATACTGCTACA[2:100[")

    private fun shortInv(position: Int = 100, otherPosition: Int = 110): VariantContext = createVariant(position, "A", "ATT]1:$otherPosition]")

    private fun shortDel(position: Int = 80, otherPosition: Int = 100): VariantContext = createVariant(position, "A", "ATACTGCTACA[1:${otherPosition}[")

    private fun shortDup(position: Int = 110, otherPosition: Int = 100): VariantContext = createVariant(position, "A", "ATACTGCTACA[1:${otherPosition}[")

    private fun createSingle(): VariantContext {
        return createVariant(1, "C", ".A")
    }

    private fun createBreakPoint(): VariantContext {
        return createVariant(1, "C", "A[2:1000[")
    }

    private fun createVariant(pos: Int, ref: String, alt: String): VariantContext {
        val line = "1\t${pos}\tid1\t${ref}\t${alt}\t100\tPASS\tinfo\tGT:BVF:VF:REF:REFPAIR\t./.:1:1:1:1\t./.:10:10:1:1"
        return codec.decode(line);
    }

    private fun VariantContext.toSv(): StructuralVariantContext = StructuralVariantContext(this)

    private fun VariantContext.qual(qual: Int): VariantContext {
        val builder = VariantContextBuilder(this)
        builder.log10PError(qual.toDouble() / -10.0)
        return builder.make()
    }

    private fun VariantContext.splitReads(normal: Int, tumor: Int): VariantContext {
        return this.addGenotypeAttribute("SR", normal, tumor);
    }

    private fun VariantContext.fragmentSupport(normal: Int, tumor: Int): VariantContext {
        return this.addGenotypeAttribute("BVF", normal, tumor).addGenotypeAttribute("VF", normal, tumor);
    }

    private fun VariantContext.setAttribute(attribute: String, value: Any): VariantContext {
        val builder = VariantContextBuilder(this)
        builder.attribute(attribute, value)
        return builder.make();
    }

    private fun VariantContext.addGenotypeAttribute(attribute: String, normal: Int, tumor: Int): VariantContext {
        val normalBuilder = GenotypeBuilder(this.getGenotype(0))
        normalBuilder.attribute(attribute, normal)

        val tumorBuilder = GenotypeBuilder(this.getGenotype(1))
        tumorBuilder.attribute(attribute, tumor)

        val builder = VariantContextBuilder(this)
        builder.genotypes(listOf(normalBuilder.make(), tumorBuilder.make()))
        return builder.make();
    }

}
