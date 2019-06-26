package com.hartwig.hmftools.patientdb.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.hartwig.hmftools.common.actionability.EvidenceItem;
import com.hartwig.hmftools.common.amber.AmberBAF;
import com.hartwig.hmftools.common.chord.ChordAnalysis;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.ecrf.EcrfModel;
import com.hartwig.hmftools.common.ecrf.datamodel.ValidationFinding;
import com.hartwig.hmftools.common.metrics.WGSMetrics;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.purple.gene.GeneCopyNumber;
import com.hartwig.hmftools.common.purple.purity.FittedPurity;
import com.hartwig.hmftools.common.purple.purity.PurityContext;
import com.hartwig.hmftools.common.purple.qc.PurpleQC;
import com.hartwig.hmftools.common.region.CanonicalTranscript;
import com.hartwig.hmftools.common.variant.EnrichedSomaticVariant;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantData;
import com.hartwig.hmftools.common.variant.structural.annotation.ReportableGeneFusion;
import com.hartwig.hmftools.common.variant.structural.linx.LinxCluster;
import com.hartwig.hmftools.common.variant.structural.linx.LinxLink;
import com.hartwig.hmftools.common.variant.structural.linx.LinxSvData;
import com.hartwig.hmftools.common.variant.structural.linx.LinxViralInsertFile;
import com.hartwig.hmftools.patientdb.data.Patient;
import com.hartwig.hmftools.patientdb.data.SampleData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public class DatabaseAccess implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger(DatabaseAccess.class);
    private static final String DEV_CATALOG = "hmfpatients_test";

    @NotNull
    private final EcrfDAO ecrfDAO;
    @NotNull
    private final AmberDAO amberDAO;
    @NotNull
    private final DSLContext context;
    @NotNull
    private final PurityDAO purityDAO;
    @NotNull
    private final MetricDAO metricDAO;
    @NotNull
    private final ClinicalDAO clinicalDAO;
    @NotNull
    private final CopyNumberDAO copyNumberDAO;
    @NotNull
    private final DriverCatalogDAO driverCatalogDAO;
    @NotNull
    private final GeneCopyNumberDAO geneCopyNumberDAO;
    @NotNull
    private final SomaticVariantDAO somaticVariantDAO;
    @NotNull
    private final StructuralVariantDAO structuralVariantDAO;
    @NotNull
    private final StructuralVariantClusterDAO structuralVariantClusterDAO;
    @NotNull
    private final StructuralVariantFusionDAO structuralVariantFusionDAO;
    @NotNull
    private final ValidationFindingDAO validationFindingsDAO;
    @NotNull
    private final CanonicalTranscriptDAO canonicalTranscriptDAO;
    @NotNull
    private final ChordDAO chordDAO;
    @NotNull
    private final ClinicalEvidenceDAO clinicalEvidenceDAO;

    public DatabaseAccess(@NotNull final String userName, @NotNull final String password, @NotNull final String url) throws SQLException {
        // Disable annoying jooq self-ad message
        System.setProperty("org.jooq.no-logo", "true");
        final Connection conn = DriverManager.getConnection(url, userName, password);
        final String catalog = conn.getCatalog();
        LOGGER.debug("Connecting to database {}", catalog);
        this.context = DSL.using(conn, SQLDialect.MYSQL, settings(catalog));

        purityDAO = new PurityDAO(context);
        amberDAO = new AmberDAO(context);
        copyNumberDAO = new CopyNumberDAO(context);
        geneCopyNumberDAO = new GeneCopyNumberDAO(context);
        somaticVariantDAO = new SomaticVariantDAO(context);
        structuralVariantDAO = new StructuralVariantDAO(context);
        structuralVariantClusterDAO = new StructuralVariantClusterDAO(context);
        structuralVariantFusionDAO = new StructuralVariantFusionDAO(context);
        ecrfDAO = new EcrfDAO(context);
        clinicalDAO = new ClinicalDAO(context);
        validationFindingsDAO = new ValidationFindingDAO(context);
        canonicalTranscriptDAO = new CanonicalTranscriptDAO(context);
        metricDAO = new MetricDAO(context);
        driverCatalogDAO = new DriverCatalogDAO(context);
        chordDAO = new ChordDAO(context);
        clinicalEvidenceDAO = new ClinicalEvidenceDAO(context);
    }

    @NotNull
    public DSLContext context() {
        return context;
    }

    @Nullable
    private static Settings settings(@NotNull String catalog) {
        return !catalog.equals(DEV_CATALOG)
                ? new Settings().withRenderMapping(new RenderMapping().withSchemata(new MappedSchema().withInput(DEV_CATALOG)
                .withOutput(catalog)))
                : null;
    }

    @Nullable
    public String readTumorLocation(@NotNull String sample) {
        return clinicalDAO.readTumorLocationForSample(sample);
    }

    public void writeCanonicalTranscripts(@NotNull final String assembly, @NotNull final List<CanonicalTranscript> transcripts) {
        canonicalTranscriptDAO.write(assembly, transcripts);
    }

    public void writePurity(@NotNull final String sampleId, @NotNull final PurityContext context, @NotNull final PurpleQC checks) {
        purityDAO.write(sampleId, context, checks);
    }

    public void writeBestFitPerPurity(@NotNull final String sampleId, @NotNull final List<FittedPurity> bestFitPerPurity) {
        purityDAO.write(sampleId, bestFitPerPurity);
    }

    public static final double MIN_SAMPLE_PURITY = 0.195;

    @NotNull
    public final List<String> getSamplesPassingQC(double minPurity) {
        return purityDAO.getSamplesPassingQC(minPurity);
    }

    @NotNull
    public final List<String> getSampleIds() {
        return purityDAO.getSampleIds();
    }

    @Nullable
    public PurityContext readPurityContext(@NotNull final String sampleId) {
        return purityDAO.readPurityContext(sampleId);
    }

    public void writeCopynumbers(@NotNull final String sample, @NotNull List<PurpleCopyNumber> copyNumbers) {
        copyNumberDAO.writeCopyNumber(sample, copyNumbers);
    }

    public void writeAmberBAF(@NotNull final String sampleId, @NotNull final List<AmberBAF> amber) {
        amberDAO.write(sampleId, amber);
    }

    public void writeSomaticVariants(@NotNull final String sampleId, @NotNull List<EnrichedSomaticVariant> variants) {
        somaticVariantDAO.write(sampleId, variants);
    }

    @NotNull
    public List<String> somaticVariantSampleList() {
        return somaticVariantDAO.getSamplesList();
    }

    public void writeStructuralVariants(@NotNull final String sampleId, @NotNull final List<StructuralVariantData> variants) {
        structuralVariantDAO.write(sampleId, variants);
    }

    @NotNull
    public List<StructuralVariantData> readStructuralVariantData(@NotNull final String sample) {
        return structuralVariantDAO.read(sample);
    }

    @NotNull
    public List<String> structuralVariantSampleList(@NotNull final String sampleSearch) {
        return structuralVariantDAO.getSamplesList(sampleSearch);
    }

    public void writeGermlineCopynumbers(@NotNull final String sample, @NotNull List<PurpleCopyNumber> copyNumbers) {
        copyNumberDAO.writeGermlineCopyNumber(sample, copyNumbers);
    }

    public void writeSvClusters(@NotNull final String sample, @NotNull List<LinxCluster> clusters) {
        structuralVariantClusterDAO.writeClusters(sample, clusters);
    }

    public void writeSvLinxData(@NotNull final String sample, @NotNull List<LinxSvData> svData) {
        structuralVariantClusterDAO.writeSvData(sample, svData);
    }

    public void writeSvLinks(@NotNull final String sample, @NotNull List<LinxLink> links) {
        structuralVariantClusterDAO.writeLinks(sample, links);
    }

    public void writeSvViralInserts(@NotNull final String sample, @NotNull List<LinxViralInsertFile> inserts) {
        structuralVariantClusterDAO.writeViralInserts(sample, inserts);
    }

    @NotNull
    public List<EnrichedSomaticVariant> readSomaticVariants(@NotNull final String sample) {
        return somaticVariantDAO.read(sample);
    }

    @NotNull
    public List<ReportableGeneFusion> readGeneFusions(@NotNull final String sample) {
        return structuralVariantFusionDAO.readGeneFusions(sample);
    }

    @NotNull
    public List<GeneCopyNumber> readGeneCopynumbers(@NotNull final String sample) {
        return geneCopyNumberDAO.read(sample);
    }

    public void writeGeneCopynumberRegions(@NotNull final String sample, @NotNull List<GeneCopyNumber> geneCopyNumbers) {
        geneCopyNumberDAO.writeCopyNumber(sample, geneCopyNumbers);
    }

    public void writeDriverCatalog(@NotNull final String sample, @NotNull List<DriverCatalog> driverCatalog) {
        driverCatalogDAO.write(sample, driverCatalog);
    }

    @NotNull
    public final List<DriverCatalog> readDriverCatalog(@NotNull final String sample) {
        return driverCatalogDAO.readDriverData(sample);
    }

    @NotNull
    public List<PurpleCopyNumber> readCopynumbers(@NotNull final String sample) {
        return copyNumberDAO.read(sample);
    }

    public void writeMetrics(@NotNull String sample, @NotNull WGSMetrics metrics) {
        metricDAO.writeMetrics(sample, metrics);
    }

    public void writeChord(@NotNull String sample, @NotNull ChordAnalysis chordAnalysis) {
        chordDAO.writeChord(sample, chordAnalysis);
    }

    public void writeClinicalEvidence(@NotNull String sample, @NotNull List<EvidenceItem> items) {
        clinicalEvidenceDAO.writeClinicalEvidence(sample, items);
    }

    public void clearCpctEcrf() {
        ecrfDAO.clearCpct();
    }

    public void clearDrupEcrf() {
        ecrfDAO.clearDrup();
    }

    public void clearClinicalTables() {
        validationFindingsDAO.clear();
        clinicalDAO.clear();
    }

    public void writeFullClinicalData(@NotNull Patient patient) {
        clinicalDAO.writeFullClinicalData(patient);
    }

    public void writeSampleClinicalData(@NotNull String patientIdentifier, @NotNull List<SampleData> samples) {
        clinicalDAO.writeSampleClinicalData(patientIdentifier, samples);
    }

    public void writeDrupEcrf(@NotNull final EcrfModel model, @NotNull final Set<String> sequencedPatients) {
        LOGGER.info("Writing DRUP datamodel...");
        ecrfDAO.writeDrupDatamodel(model.fields());
        LOGGER.info(" Done writing DRUP datamodel.");
        LOGGER.info("Writing DRUP patients...");
        model.patients().forEach(patient -> ecrfDAO.writeDrupPatient(patient, sequencedPatients.contains(patient.patientId())));
        LOGGER.info(" Done writing DRUP patients.");
    }

    public void writeCpctEcrf(@NotNull final EcrfModel model, @NotNull final Set<String> sequencedPatients) {
        LOGGER.info("Writing CPCT datamodel...");
        ecrfDAO.writeCpctDatamodel(model.fields());
        LOGGER.info("Done writing CPCT datamodel.");
        LOGGER.info("Writing CPCT patients...");
        model.patients().forEach(patient -> ecrfDAO.writeCpctPatient(patient, sequencedPatients.contains(patient.patientId())));
        LOGGER.info("Done writing CPCT patients.");
    }

    public void writeValidationFindings(@NotNull final List<ValidationFinding> findings) {
        validationFindingsDAO.write(findings);
    }

    public void deleteAllDataForSample(@NotNull String sample) {
        LOGGER.info("Starting deleting data");

        LOGGER.info("Deleting metric data for sample: " + sample);
        metricDAO.deleteMetricForSample(sample);

        LOGGER.info("Deleting chord data for sample: " + sample);
        chordDAO.deleteChordForSample(sample);

        LOGGER.info("Deleting amber data for sample: " + sample);
        amberDAO.deleteAmberRecordsForSample(sample);

        LOGGER.info("Deleting purity data for sample: " + sample);
        purityDAO.deletePurityForSample(sample);

        LOGGER.info("Deleting copy number data for sample: " + sample);
        copyNumberDAO.deleteCopyNumberForSample(sample);

        LOGGER.info("Deleting gene copy numbers for sample: " + sample);
        geneCopyNumberDAO.deleteGeneCopyNumberForSample(sample);

        LOGGER.info("Deleting somatic variants for sample: " + sample);
        somaticVariantDAO.deleteSomaticVariantForSample(sample);

        LOGGER.info("Deleting structural variant annotation data for sample: " + sample);
        structuralVariantFusionDAO.deleteAnnotationsForSample(sample);

        LOGGER.info("Deleting structural variant cluster data for sample: " + sample);
        structuralVariantClusterDAO.deleteClusterDataForSample(sample);

        LOGGER.info("Deleting structural variants for sample: " + sample);
        structuralVariantDAO.deleteStructuralVariantsForSample(sample);

        LOGGER.info("Deleting evidence data for sample: " + sample);
        clinicalEvidenceDAO.deleteClinicalEvidenceForSample(sample);

        LOGGER.info("Deleting driver catalog for sample: " + sample);
        driverCatalogDAO.deleteForSample(sample);

        LOGGER.info("All data for sample: " + sample + " has been deleted");
    }

    @Override
    public void close() {
        context.close();
    }
}

