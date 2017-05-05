package com.hartwig.hmftools.patientdb.matchers;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.patientdb.data.BiopsyClinicalData;
import com.hartwig.hmftools.patientdb.data.BiopsyTreatmentData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TreatmentMatcher {
    private static final Logger LOGGER = LogManager.getLogger(TreatmentMatcher.class);

    @NotNull
    public static List<BiopsyTreatmentData> matchTreatments(@NotNull final String patientId,
            @NotNull final List<BiopsyClinicalData> biopsies, @NotNull final List<BiopsyTreatmentData> treatments) {
        if (biopsies.size() < treatments.size()) {
            if (biopsies.size() < treatments.size()) {
                LOGGER.warn("Patient " + patientId + " has more treatments than biopsies.");
            }
        }
        if (biopsies.size() == 1 && treatments.size() > 1) {
            // MIVO: if we have just 1 biopsy, add all treatments (although this should not happen and be fixed in the ecrf).
            final List<BiopsyTreatmentData> matchedTreatments = Lists.newArrayList();
            for (final BiopsyTreatmentData treatment : treatments) {
                matchedTreatments.add(
                        new BiopsyTreatmentData(treatment.id(), treatment.treatmentGiven(), treatment.startDate(),
                                treatment.endDate(), treatment.drugs(), biopsies.get(0).id()));
            }
            return matchedTreatments;
        } else {
            return matchTreatmentsByIndex(biopsies, treatments);
        }
    }

    @NotNull
    private static List<BiopsyTreatmentData> matchTreatmentsByIndex(@NotNull final List<BiopsyClinicalData> biopsies,
            @NotNull final List<BiopsyTreatmentData> treatments) {
        final List<BiopsyTreatmentData> matchedTreatments = Lists.newArrayList();
        for (int index = 0; index < treatments.size(); index++) {
            final Integer biopsyId;
            final BiopsyTreatmentData treatment = treatments.get(index);
            if (index < biopsies.size()) {
                biopsyId = biopsies.get(index).id();
                checkDurationBetweenDates(biopsies.get(index).date(), treatment.startDate());
            } else {
                biopsyId = null;
            }
            matchedTreatments.add(
                    new BiopsyTreatmentData(treatment.id(), treatment.treatmentGiven(), treatment.startDate(),
                            treatment.endDate(), treatment.drugs(), biopsyId));
        }
        return matchedTreatments;
    }

    private static void checkDurationBetweenDates(@Nullable final LocalDate biopsyDate,
            @Nullable final LocalDate treatmentStartDate) {
        final int maxDaysBetweenTreatmentAndBiopsy = 90;
        if (biopsyDate != null && treatmentStartDate != null) {
            if (Duration.between(biopsyDate.atStartOfDay(), treatmentStartDate.atStartOfDay()).toDays()
                    > maxDaysBetweenTreatmentAndBiopsy) {
                LOGGER.warn("Time between biopsy date and treatment start date is greater than "
                        + maxDaysBetweenTreatmentAndBiopsy + " days.");
            }
        }
    }

}
