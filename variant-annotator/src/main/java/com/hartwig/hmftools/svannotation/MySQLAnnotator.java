package com.hartwig.hmftools.svannotation;

import static org.ensembl.database.homo_sapiens_core.Tables.COORD_SYSTEM;
import static org.ensembl.database.homo_sapiens_core.Tables.EXON;
import static org.ensembl.database.homo_sapiens_core.Tables.EXON_TRANSCRIPT;
import static org.ensembl.database.homo_sapiens_core.Tables.GENE;
import static org.ensembl.database.homo_sapiens_core.Tables.OBJECT_XREF;
import static org.ensembl.database.homo_sapiens_core.Tables.SEQ_REGION;
import static org.ensembl.database.homo_sapiens_core.Tables.TRANSCRIPT;
import static org.ensembl.database.homo_sapiens_core.Tables.XREF;
import static org.jooq.impl.DSL.decode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.structural.StructuralVariant;
import com.hartwig.hmftools.svannotation.annotations.GeneAnnotation;
import com.hartwig.hmftools.svannotation.annotations.StructuralVariantAnnotation;
import com.hartwig.hmftools.svannotation.annotations.Transcript;

import org.ensembl.database.homo_sapiens_core.enums.GeneStatus;
import org.ensembl.database.homo_sapiens_core.enums.ObjectXrefEnsemblObjectType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;

public class MySQLAnnotator implements VariantAnnotator {

    private final DSLContext context;
    private final UInteger coord_system_id;

    public static VariantAnnotator make(final String url) throws SQLException {
        return new MySQLAnnotator(url);
    }

    private MySQLAnnotator(final String url) throws SQLException {
        System.setProperty("org.jooq.no-logo", "true");
        final Connection conn = DriverManager.getConnection(url);
        context = DSL.using(conn, SQLDialect.MYSQL);
        coord_system_id = findCoordSystemId();
    }

    private UInteger findCoordSystemId() {
        return context.select(COORD_SYSTEM.COORD_SYSTEM_ID)
                .from(COORD_SYSTEM)
                .where(COORD_SYSTEM.VERSION.eq("GRCh37"))
                .orderBy(COORD_SYSTEM.RANK)
                .limit(1)
                .fetchOne()
                .value1();
    }

    @Override
    public List<StructuralVariantAnnotation> annotateVariants(final List<StructuralVariant> variants) {
        return variants.stream().map(this::annotateVariant).collect(Collectors.toList());
    }

    private StructuralVariantAnnotation annotateVariant(final StructuralVariant variant) {
        final StructuralVariantAnnotation annotation = new StructuralVariantAnnotation(variant);
        annotation.getAnnotations().addAll(annotateBreakend(annotation, true, variant.startChromosome(), variant.startPosition()));
        annotation.getAnnotations().addAll(annotateBreakend(annotation, false, variant.endChromosome(), variant.endPosition()));
        return annotation;
    }

    private List<GeneAnnotation> annotateBreakend(final StructuralVariantAnnotation parent, final boolean isStart, final String chromosome,
            final long position) {

        final List<GeneAnnotation> result = Lists.newArrayList();

        final int PROMOTER_DISTANCE = 10000;
        final byte zero = 0;

        // start with the overlapping genes
        final Result<?> genes =
                context.select(GENE.GENE_ID, XREF.DISPLAY_LABEL, GENE.STABLE_ID, GENE.CANONICAL_TRANSCRIPT_ID, GENE.SEQ_REGION_STRAND)
                        .from(GENE)
                        .innerJoin(SEQ_REGION)
                        .on(GENE.SEQ_REGION_ID.eq(SEQ_REGION.SEQ_REGION_ID))
                        .and(SEQ_REGION.NAME.eq(chromosome))
                        .and(SEQ_REGION.COORD_SYSTEM_ID.eq(coord_system_id))
                        .innerJoin(XREF)
                        .on(XREF.XREF_ID.eq(GENE.DISPLAY_XREF_ID))
                        .where(GENE.STATUS.eq(GeneStatus.KNOWN))
                        .and(decode().when(GENE.SEQ_REGION_STRAND.gt(zero),
                                decode().when(GENE.SEQ_REGION_START.ge(UInteger.valueOf(PROMOTER_DISTANCE)),
                                        GENE.SEQ_REGION_START.sub(PROMOTER_DISTANCE)).otherwise(GENE.SEQ_REGION_START))
                                .otherwise(GENE.SEQ_REGION_START)
                                .le(UInteger.valueOf(position)))
                        .and(decode().when(GENE.SEQ_REGION_STRAND.lt(zero), GENE.SEQ_REGION_END.add(PROMOTER_DISTANCE))
                                .otherwise(GENE.SEQ_REGION_END)
                                .ge(UInteger.valueOf(position)))
                        .fetch();

        for (final Record g : genes) {
            final UInteger gene_id = g.get(GENE.GENE_ID);
            final String gene_name = g.get(XREF.DISPLAY_LABEL);
            final String gene_stable_id = g.get(GENE.STABLE_ID);
            final UInteger canonical_transcript_id = g.get(GENE.CANONICAL_TRANSCRIPT_ID);
            final int gene_strand = g.get(GENE.SEQ_REGION_STRAND);

            final List<String> synonyms = context.select(XREF.DBPRIMARY_ACC)
                    .from(XREF)
                    .innerJoin(OBJECT_XREF)
                    .on(OBJECT_XREF.XREF_ID.eq(XREF.XREF_ID))
                    .and(OBJECT_XREF.ENSEMBL_ID.eq(gene_id))
                    .and(OBJECT_XREF.ENSEMBL_OBJECT_TYPE.eq(ObjectXrefEnsemblObjectType.Gene))
                    .fetch()
                    .stream()
                    .map(r -> r.get(XREF.DBPRIMARY_ACC))
                    .collect(Collectors.toList());

            final GeneAnnotation geneAnnotation = new GeneAnnotation(parent, isStart, gene_name, synonyms, gene_stable_id, gene_strand);

            final Result<?> transcripts = context.select(TRANSCRIPT.TRANSCRIPT_ID, TRANSCRIPT.STABLE_ID)
                    .from(TRANSCRIPT)
                    .where(TRANSCRIPT.GENE_ID.eq(gene_id))
                    .fetch();

            for (final Record t : transcripts) {
                final UInteger transcript_id = t.get(TRANSCRIPT.TRANSCRIPT_ID);
                final boolean canonical = transcript_id.equals(canonical_transcript_id);
                final String transcript_stable_id = t.get(TRANSCRIPT.STABLE_ID);

                final Record exonLeft = context.select(EXON_TRANSCRIPT.RANK, EXON.PHASE, EXON.END_PHASE)
                        .from(EXON_TRANSCRIPT)
                        .innerJoin(EXON)
                        .on(EXON.EXON_ID.eq(EXON_TRANSCRIPT.EXON_ID))
                        .where(EXON_TRANSCRIPT.TRANSCRIPT_ID.eq(transcript_id))
                        .and(EXON.SEQ_REGION_START.le(UInteger.valueOf(position)))
                        .orderBy(EXON.SEQ_REGION_START.desc())
                        .limit(1)
                        .fetchOne();

                final Record exonRight = context.select(EXON_TRANSCRIPT.RANK, EXON.PHASE, EXON.END_PHASE)
                        .from(EXON_TRANSCRIPT)
                        .innerJoin(EXON)
                        .on(EXON.EXON_ID.eq(EXON_TRANSCRIPT.EXON_ID))
                        .where(EXON_TRANSCRIPT.TRANSCRIPT_ID.eq(transcript_id))
                        .and(EXON.SEQ_REGION_END.ge(UInteger.valueOf(position)))
                        .orderBy(EXON.SEQ_REGION_END.asc())
                        .limit(1)
                        .fetchOne();

                final int exon_max = context.select(EXON_TRANSCRIPT.RANK)
                        .from(EXON_TRANSCRIPT)
                        .where(EXON_TRANSCRIPT.TRANSCRIPT_ID.eq(transcript_id))
                        .orderBy(EXON_TRANSCRIPT.RANK.desc())
                        .limit(1)
                        .fetchOne()
                        .value1();

                final int exon_upstream;
                final int exon_upstream_phase;
                final int exon_downstream;
                final int exon_downstream_phase;

                if (gene_strand > 0) {
                    // forward strand
                    exon_upstream = exonLeft == null ? 0 : exonLeft.get(EXON_TRANSCRIPT.RANK);
                    exon_upstream_phase = exonLeft == null ? -1 : exonLeft.get(EXON.END_PHASE);
                    exon_downstream = exonRight == null ? 0 : exonRight.get(EXON_TRANSCRIPT.RANK);
                    exon_downstream_phase = exonRight == null ? -1 : exonRight.get(EXON.PHASE);
                } else {
                    // reverse strand
                    exon_downstream = exonLeft == null ? 0 : exonLeft.get(EXON_TRANSCRIPT.RANK);
                    exon_downstream_phase = exonLeft == null ? -1 : exonLeft.get(EXON.PHASE);
                    exon_upstream = exonRight == null ? 0 : exonRight.get(EXON_TRANSCRIPT.RANK);
                    exon_upstream_phase = exonRight == null ? -1 : exonRight.get(EXON.END_PHASE);
                }

                if (exon_upstream > 0 && exon_downstream == 0) {
                    // past the last exon
                    continue;
                }

                final Transcript transcript =
                        new Transcript(geneAnnotation, transcript_stable_id, exon_upstream, exon_upstream_phase, exon_downstream,
                                exon_downstream_phase, exon_max, canonical);
                geneAnnotation.addTranscript(transcript);
            }

            if (!geneAnnotation.getTranscripts().isEmpty()) {
                result.add(geneAnnotation);
            }
        }

        return result;
    }
}
