package com.hartwig.hmftools.common.variant.structural.linx;

import static java.util.stream.Collectors.toList;

import static com.hartwig.hmftools.common.variant.structural.linx.LinxClusterFile.DELIMITER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class LinxBreakendFile
{
    private static final String FILE_EXTENSION = ".linx.breakend.tsv";

    @NotNull
    public static String generateFilename(@NotNull final String basePath, @NotNull final String sample)
    {
        return basePath + File.separator + sample + FILE_EXTENSION;
    }

    @NotNull
    public static List<LinxBreakend> read(final String filePath) throws IOException
    {
        return fromLines(Files.readAllLines(new File(filePath).toPath()));
    }

    public static void write(@NotNull final String filename, @NotNull List<LinxBreakend> breakends) throws IOException
    {
        Files.write(new File(filename).toPath(), toLines(breakends));
    }

    @NotNull
    private static List<String> toLines(@NotNull final List<LinxBreakend> breakends)
    {
        final List<String> lines = Lists.newArrayList();
        lines.add(header());
        breakends.stream().map(x -> toString(x)).forEach(lines::add);
        return lines;
    }

    @NotNull
    private static List<LinxBreakend> fromLines(@NotNull List<String> lines)
    {
        return lines.stream().filter(x -> !x.startsWith("Id")).map(LinxBreakendFile::fromString).collect(toList());
    }

    @NotNull
    private static String header()
    {
        return new StringJoiner(DELIMITER)
                .add("Id")
                .add("SvId")
                .add("IsStart")
                .add("Gene")
                .add("TranscriptId")
                .add("Canonical")
                .add("IsUpstream")
                .add("Disruptive")
                .add("ReportedDisruption")
                .add("UndisruptedCopyNumber")
                .add("RegionType")
                .add("CodingContext")
                .add("Biotype")
                .add("ExonBasePhase")
                .add("NextSpliceRank")
                .add("NextSplicePhase")
                .add("NextSpliceDistance")
                .add("TotalExonCount")
                .toString();
    }

    @NotNull
    private static String toString(@NotNull final LinxBreakend breakend)
    {
        return new StringJoiner(DELIMITER)
                .add(String.valueOf(breakend.id()))
                .add(String.valueOf(breakend.svId()))
                .add(String.valueOf(breakend.isStart()))
                .add(String.valueOf(breakend.gene()))
                .add(String.valueOf(breakend.transcriptId()))
                .add(String.valueOf(breakend.canonical()))
                .add(String.valueOf(breakend.isUpstream()))
                .add(String.valueOf(breakend.disruptive()))
                .add(String.valueOf(breakend.reportedDisruption()))
                .add(String.valueOf(breakend.undisruptedCopyNumber()))
                .add(String.valueOf(breakend.regionType()))
                .add(String.valueOf(breakend.codingContext()))
                .add(String.valueOf(breakend.biotype()))
                .add(String.valueOf(breakend.exonBasePhase()))
                .add(String.valueOf(breakend.nextSpliceExonRank()))
                .add(String.valueOf(breakend.nextSpliceExonPhase()))
                .add(String.valueOf(breakend.nextSpliceDistance()))
                .add(String.valueOf(breakend.totalExonCount()))
                .toString();
    }

    @NotNull
    private static LinxBreakend fromString(@NotNull final String breakend)
    {
        String[] values = breakend.split(DELIMITER);

        int index = 0;

        return ImmutableLinxBreakend.builder()
                .id(Integer.parseInt(values[index++]))
                .svId(Integer.parseInt(values[index++]))
                .isStart(Boolean.parseBoolean(values[index++]))
                .gene(values[index++])
                .transcriptId(values[index++])
                .canonical(Boolean.parseBoolean(values[index++]))
                .isUpstream(Boolean.parseBoolean(values[index++]))
                .disruptive(Boolean.parseBoolean(values[index++]))
                .reportedDisruption(Boolean.parseBoolean(values[index++]))
                .undisruptedCopyNumber(Double.parseDouble(values[index++]))
                .regionType(values[index++])
                .codingContext(values[index++])
                .biotype(values[index++])
                .exonBasePhase(Integer.parseInt(values[index++]))
                .nextSpliceExonRank(Integer.parseInt(values[index++]))
                .nextSpliceExonPhase(Integer.parseInt(values[index++]))
                .nextSpliceDistance(Integer.parseInt(values[index++]))
                .totalExonCount(Integer.parseInt(values[index++]))
                .build();
    }
}
