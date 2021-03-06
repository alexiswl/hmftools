package com.hartwig.hmftools.protect.conclusion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class TemplateConclusionFile {


    private static final String DELIMITER = "\t";


    @NotNull
    public static List<TemplateConclusion> readTemplateConclusion(@NotNull String templateConclusionTsv) throws IOException {
        final List<TemplateConclusion> templateConclusion = Lists.newArrayList();
        final List<String> lineTemplates = Files.readAllLines(new File(templateConclusionTsv).toPath());
        for (String template : lineTemplates.subList(1, lineTemplates.size())) {
            templateConclusion.add(fromTemplate(template));
        }
        return templateConclusion;
    }

    @NotNull
    private static TemplateConclusion fromTemplate(@NotNull String line) {
        final String[] values = line.split(DELIMITER);

        // TODO add include/exlude tumor location, and include/extra for extra
        if (values.length == 3) {
            return ImmutableTemplateConclusion.builder()
                    .abberrationGeneSummary(values[0])
                    .targetedTherapy(values[1])
                    .summaryTextStatement(values[2])
                    .build();
        } else {
            return ImmutableTemplateConclusion.builder()
                    .abberrationGeneSummary(values[0])
                    .targetedTherapy(values[1])
                    .summaryTextStatement(values[2])
                    .summaryAdditionalText(values[3])
                    .build();
        }
    }
}
