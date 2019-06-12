package com.hartwig.hmftools.linx.visualisation.circos;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.common.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.position.GenomePosition;
import com.hartwig.hmftools.common.position.GenomePositions;
import com.hartwig.hmftools.common.region.GenomeRegion;
import com.hartwig.hmftools.common.region.GenomeRegionFactory;
import com.hartwig.hmftools.linx.visualisation.data.CopyNumberAlteration;
import com.hartwig.hmftools.linx.visualisation.data.Exon;
import com.hartwig.hmftools.linx.visualisation.data.Link;
import com.hartwig.hmftools.linx.visualisation.data.Links;
import com.hartwig.hmftools.linx.visualisation.data.Segment;

import org.jetbrains.annotations.NotNull;

public class CircosDataWriter
{

    private static DecimalFormat POSITION_FORMAT = new DecimalFormat("#,###");

    private static final int MIN_KAROTYPE_LENGTH = 10;
    private static final String DELIMITER = "\t";

    private final ColorPicker colorPicker;
    private final String filePrefix;
    private final int maxTracks;
    private boolean debug;

    public CircosDataWriter(final boolean debug, final ColorPicker colorPicker, @NotNull final String sample,
            @NotNull final String outputDir, final int maxTracks)
    {
        this.debug = debug;
        this.colorPicker = colorPicker;
        this.filePrefix = outputDir + File.separator + sample;
        this.maxTracks = maxTracks;
    }

    public void write(@NotNull final List<Segment> unadjustedSegments, @NotNull final List<Link> unadjustedLinks,
            @NotNull final List<CopyNumberAlteration> unadjustedAlterations, @NotNull final List<Exon> unadjustedExons) throws IOException
    {

        final List<GenomeRegion> unadjustedFragileSites =
                Highlights.limitHighlightsToSegments(Highlights.fragileSites(), unadjustedSegments);

        final List<GenomeRegion> unadjustedLineElements =
                Highlights.limitHighlightsToSegments(Highlights.lineElements(), unadjustedSegments);

        // Note we do not add exons here because we want them interpolated.
        final List<GenomePosition> unadjustedPositions = Lists.newArrayList();
        unadjustedPositions.addAll(Links.allPositions(unadjustedLinks));
        unadjustedPositions.addAll(Span.allPositions(unadjustedSegments));
        unadjustedPositions.addAll(Span.allPositions(unadjustedAlterations));
        unadjustedPositions.addAll(Span.allPositions(unadjustedFragileSites));
        unadjustedPositions.addAll(Span.allPositions(unadjustedLineElements));

        final ScalePosition scalePosition = new ScalePosition(unadjustedPositions);
        final List<GenomePosition> scaledPositions = scalePosition.scaled();
        final Map<String, Integer> contigLengths = contigLengths(scaledPositions);

        final List<Segment> segments = scalePosition.scaleSegments(unadjustedSegments);
        final List<Link> links = scalePosition.scaleLinks(unadjustedLinks);
        final List<CopyNumberAlteration> alterations = scalePosition.scaleAlterations(unadjustedAlterations);
        final List<GenomeRegion> fragileSites = scalePosition.scaleRegions(unadjustedFragileSites);
        final List<GenomeRegion> lineElements = scalePosition.scaleRegions(unadjustedLineElements);
        final List<Exon> exons = scalePosition.interpolateExons(unadjustedExons);

        final String exonPath = filePrefix + ".exon.circos";
        Files.write(new File(exonPath).toPath(), exons(exons));

        final String genePath = filePrefix + ".gene.circos";
        Files.write(new File(genePath).toPath(), genes(exons));

        final String geneNamePath = filePrefix + ".gene.name.circos";
        Files.write(new File(geneNamePath).toPath(), geneName(exons));

        final String textPath = filePrefix + ".text.circos";
        Files.write(new File(textPath).toPath(), createPositionText(debug, unadjustedLinks, links, segments));

        final String histogramPath = filePrefix + ".segment.circos";
        Files.write(new File(histogramPath).toPath(), createHistogramTrack(segments));

        final String karyotypePath = filePrefix + ".karyotype.circos";
        Files.write(new File(karyotypePath).toPath(), createKaryotypes(contigLengths));

        final String connectorPath = filePrefix + ".connector.circos";
        Files.write(new File(connectorPath).toPath(), createConnectors(maxTracks, segments, links));

        final String linkPath = filePrefix + ".link.circos";
        Files.write(new File(linkPath).toPath(), createLinks(links));

        final String scatterPath = filePrefix + ".scatter.circos";
        Files.write(new File(scatterPath).toPath(), createScatter(segments, links));

        final String cnaPath = filePrefix + ".cna.circos";
        Files.write(new File(cnaPath).toPath(), createCNA(alterations));

        final String mapPath = filePrefix + ".map.circos";
        Files.write(new File(mapPath).toPath(), createMinorAllelePloidy(alterations));

        final String fragile = filePrefix + ".fragile.circos";
        Files.write(new File(fragile).toPath(), highlights(fragileSites));

        final String line = filePrefix + ".line_element.circos";
        Files.write(new File(line).toPath(), highlights(lineElements));

        final String distances = filePrefix + ".distance.circos";
        if (unadjustedAlterations.size() < 200)
        {
            Files.write(new File(distances).toPath(), createDistances(unadjustedAlterations, alterations));
        }
        else
        {
            Files.write(new File(distances).toPath(), Collections.emptySet());
        }

    }

    @NotNull
    private List<String> genes(@NotNull final List<Exon> exons)
    {
        final List<String> result = Lists.newArrayList();
        final Set<String> genes = exons.stream().map(Exon::gene).collect(Collectors.toSet());
        for (final String gene : genes)
        {
            final List<Exon> geneExons = exons.stream().filter(x -> x.gene().equals(gene)).collect(toList());
            long min = geneExons.stream().mapToLong(GenomeRegion::start).min().orElse(0);
            long max = geneExons.stream().mapToLong(GenomeRegion::end).max().orElse(0);

            final String exonString = new StringJoiner(DELIMITER).add(circosContig(geneExons.get(0).chromosome()))
                    .add(String.valueOf(min))
                    .add(String.valueOf(max))
                    .add(String.valueOf(1))
                    .toString();
            result.add(exonString);

        }

        return result;
    }

    @NotNull
    private List<String> geneName(@NotNull final List<Exon> exons)
    {
        final List<String> result = Lists.newArrayList();
        final Set<String> genes = exons.stream().map(Exon::gene).collect(Collectors.toSet());
        for (final String gene : genes)
        {
            final List<Exon> geneExons = exons.stream().filter(x -> x.gene().equals(gene)).collect(toList());
            long min = geneExons.stream().mapToLong(GenomeRegion::start).min().orElse(0);

            final String geneName = geneExons.get(0).gene();
            final double labelSize = geneNameLabelSize(geneName);

            final String exonString = new StringJoiner(DELIMITER).add(circosContig(geneExons.get(0).chromosome()))
                    .add(String.valueOf(min))
                    .add(String.valueOf(min))
                    .add(geneName)
                    .add("label_size=" + labelSize + "p,rpadding=0r")
                    .toString();
            result.add(exonString);
        }

        return result;
    }

    private static double geneNameLabelSize(@NotNull final String gene)
    {
        double availablePixels = CircosConfigWriter.PIXELS * (CircosConfigWriter.EXON_OUTER_RADIUS - CircosConfigWriter.EXON_INNER_RADIUS);
        return Math.min(26, 4 + Math.floor(availablePixels / gene.length()));
    }

    @NotNull
    private List<String> exons(@NotNull final List<Exon> exons)
    {
        final List<String> result = Lists.newArrayList();
        final Set<String> chromosome = exons.stream().map(GenomeRegion::chromosome).collect(Collectors.toSet());
        for (String contig : chromosome)
        {

            final List<GenomeRegion> contigRegions = exons.stream()
                    .filter(x -> x.chromosome().equals(contig))
                    .map(x -> GenomeRegionFactory.create(x.chromosome(), x.start(), x.end()))
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            for (int i = 0; i < contigRegions.size(); i++)
            {
                final GenomeRegion region = contigRegions.get(i);
                final String exonString = new StringJoiner(DELIMITER).add(circosContig(region.chromosome()))
                        .add(String.valueOf(region.start()))
                        .add(String.valueOf(region.end()))
                        .add(String.valueOf(1))
                        .toString();
                result.add(exonString);

                if (i < contigRegions.size() - 1)
                {
                    final GenomeRegion next = contigRegions.get(i + 1);

                    final String betweenString = new StringJoiner(DELIMITER).add(circosContig(region.chromosome()))
                            .add(String.valueOf(region.end()))
                            .add(String.valueOf(next.start()))
                            .add(String.valueOf(0))
                            .toString();
                    //                    result.add(betweenString);
                }
            }
        }

        return result;
    }

    @NotNull
    private List<String> highlights(@NotNull final List<GenomeRegion> regions)
    {
        return regions.stream()
                .map(x -> new StringJoiner(DELIMITER).add(circosContig(x.chromosome()))
                        .add(String.valueOf(x.start()))
                        .add(String.valueOf(x.end()))
                        .toString())
                .collect(toList());
    }

    @NotNull
    private List<String> createDistances(@NotNull final List<CopyNumberAlteration> unadjustedSegment,
            @NotNull final List<CopyNumberAlteration> segments)
    {

        final long labelSize;
        if (segments.size() < 50)
        {
            labelSize = 30;
        }
        else if (segments.size() < 100)
        {
            labelSize = 25;
        }
        else
        {
            labelSize = 20;
        }

        final List<String> result = Lists.newArrayList();
        for (int i = 0; i < unadjustedSegment.size(); i++)
        {
            final CopyNumberAlteration adjusted = segments.get(i);
            final CopyNumberAlteration unadjusted = unadjustedSegment.get(i);

            final String distance = new StringJoiner(DELIMITER).add(circosContig(adjusted.chromosome()))
                    .add(String.valueOf(adjusted.start()))
                    .add(String.valueOf(adjusted.end()))
                    .add(shorthand(unadjusted.end() - unadjusted.start()))
                    .add("labelSize=" + labelSize + "p")
                    .toString();
            result.add(distance);
        }
        return result;
    }

    @NotNull
    private List<String> createCNA(@NotNull final List<CopyNumberAlteration> alterations)
    {
        final List<String> result = Lists.newArrayList();
        for (CopyNumberAlteration alteration : alterations)
        {
            final String cna = new StringJoiner(DELIMITER).add(circosContig(alteration.chromosome()))
                    .add(String.valueOf(alteration.start()))
                    .add(String.valueOf(alteration.end()))
                    .add(String.valueOf(alteration.copyNumber() - 2))
                    .toString();
            result.add(cna);
        }

        return result;
    }

    @NotNull
    private List<String> createMinorAllelePloidy(@NotNull final List<CopyNumberAlteration> alterations)
    {
        final List<String> result = Lists.newArrayList();
        for (CopyNumberAlteration alteration : alterations)
        {
            final String cna = new StringJoiner(DELIMITER).add(circosContig(alteration.chromosome()))
                    .add(String.valueOf(alteration.start()))
                    .add(String.valueOf(alteration.end()))
                    .add(String.valueOf(alteration.minorAllelePloidy() - 1))
                    .toString();
            result.add(cna);
        }

        return result;
    }

    @NotNull
    private List<String> createScatter(@NotNull final List<Segment> segments, @NotNull final List<Link> links)
    {
        final List<String> result = Lists.newArrayList();
        for (Segment segment : segments)
        {

            if (segment.track() == 0)
            {
                continue;
            }

            final String colorOption = colorPicker.color(segment.clusterId(), segment.chainId());
            final String startGlyph = scatterGlyph(true, segment, links);
            result.add(scatterEntry(true, segment, colorOption, startGlyph, 20));
            if (segment.startTerminal() == SegmentTerminal.CENTROMERE)
            {
                result.add(scatterEntry(true, segment, "color=white", startGlyph, 14));
            }

            final String endGlyph = scatterGlyph(false, segment, links);
            result.add(scatterEntry(false, segment, colorOption, endGlyph, 20));
            if (segment.endTerminal() == SegmentTerminal.CENTROMERE)
            {
                result.add(scatterEntry(false, segment, "color=white", endGlyph, 14));
            }
        }

        return result;
    }

    @NotNull
    private String scatterGlyph(boolean isStart, @NotNull final Segment segment, @NotNull final List<Link> links)
    {
        long location = isStart ? segment.start() : segment.end();
        final SegmentTerminal terminal = isStart ? segment.startTerminal() : segment.endTerminal();
        if (terminal != SegmentTerminal.NONE)
        {
            return "square";
        }

        final GenomePosition startPosition = GenomePositions.create(segment.chromosome(), location);
        final boolean isFoldback =
                Links.findStartLink(startPosition, links).filter(x -> x.startInfo().equals("FOLDBACK")).isPresent() || Links.findEndLink(
                        startPosition,
                        links).filter(x -> x.endInfo().equals("FOLDBACK")).isPresent();

        return isFoldback ? "triangle" : "circle";
    }

    @NotNull
    private String scatterEntry(boolean isStart, @NotNull final Segment segment, @NotNull final String color, @NotNull final String glyph,
            int glyph_size)
    {

        long location = isStart ? segment.start() : segment.end();

        return new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                .add(String.valueOf(location))
                .add(String.valueOf(location))
                .add(String.valueOf(segment.track()))
                .add(color + "," + "glyph=" + glyph + ",glyph_size=" + glyph_size)
                .toString();
    }

    @NotNull
    private List<String> createLinks(@NotNull final List<Link> links)
    {
        final List<String> result = Lists.newArrayList();
        for (final Link link : Links.clean(links))
        {

            final String linkString = new StringJoiner(DELIMITER).add(circosContig(link.startChromosome()))
                    .add(String.valueOf(link.startPosition()))
                    .add(String.valueOf(link.startPosition()))
                    .add(circosContig(link.endChromosome()))
                    .add(String.valueOf(link.endPosition()))
                    .add(String.valueOf(link.endPosition()))
                    .add(colorPicker.transparentColor(link.clusterId(), link.chainId()) + "," + thicknessString(link.traverseCount()))
                    .toString();
            result.add(linkString);
        }

        return result;
    }

    @NotNull
    private List<String> createConnectors(int maxTracks, @NotNull final List<Segment> segments, @NotNull final List<Link> links)
    {
        final List<String> result = Lists.newArrayList();

        for (Segment segment : segments)
        {

            final GenomePosition startPosition = GenomePositions.create(segment.chromosome(), segment.start());

            final double r1 = CircosConfigWriter.svTrackRelative(maxTracks, segment.track());
            int startLinkUsage = Links.linkTraverseCount(startPosition, links);

            if (startLinkUsage > 0)
            {
                long segmentsBelow = segments.stream()
                        .filter(x -> x.chromosome().equals(segment.chromosome()) && x.start() == segment.start()
                                && x.track() < segment.track())
                        .count();

                final String start = new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                        .add(String.valueOf(segment.start()))
                        .add(String.valueOf(segment.start()))
                        .add("r1=" + r1 + "r," + colorPicker.transparentColor(segment.clusterId(), segment.chainId()) + ","
                                + thicknessString(startLinkUsage - segmentsBelow))
                        .toString();
                result.add(start);
            }

            final GenomePosition endPosition = GenomePositions.create(segment.chromosome(), segment.end());

            int endLinkUsage = Links.linkTraverseCount(endPosition, links);

            if (endLinkUsage > 0)
            {
                long segmentsBelow = segments.stream()
                        .filter(x -> x.chromosome().equals(segment.chromosome()) && x.end() == segment.end() && x.track() < segment.track())
                        .count();
                final String end = new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                        .add(String.valueOf(segment.end()))
                        .add(String.valueOf(segment.end()))
                        .add("r1=" + r1 + "r," + colorPicker.transparentColor(segment.clusterId(), segment.chainId()) + ","
                                + thicknessString(endLinkUsage - segmentsBelow))
                        .toString();
                result.add(end);
            }

        }

        double rTrack1 = CircosConfigWriter.svTrackRelative(maxTracks, 0);
        for (Link link : links)
        {
            if (link.connectorsOnly())
            {
                if (link.isValidStart())
                {
                    final String start = new StringJoiner(DELIMITER).add(circosContig(link.startChromosome()))
                            .add(String.valueOf(link.startPosition()))
                            .add(String.valueOf(link.startPosition()))
                            .add("r1=" + rTrack1 + "r," + colorPicker.transparentColor(link.clusterId(), link.chainId()) + ","
                                    + thicknessString(link.traverseCount()))
                            .toString();
                    result.add(start);
                }

                if (link.isValidEnd())
                {
                    final String end = new StringJoiner(DELIMITER).add(circosContig(link.endChromosome()))
                            .add(String.valueOf(link.endPosition()))
                            .add(String.valueOf(link.endPosition()))
                            .add("r1=" + rTrack1 + "r," + colorPicker.transparentColor(link.clusterId(), link.chainId()) + ","
                                    + thicknessString(link.traverseCount()))
                            .toString();
                    result.add(end);
                }

            }

        }

        return result;
    }

    @NotNull
    private List<String> createKaryotypes(@NotNull final Map<String, Integer> contigLengths)
    {
        final List<String> result = Lists.newArrayList();
        for (String contig : contigLengths.keySet())
        {

            final String start = new StringJoiner(" ").add("chr -")
                    .add(circosContig(contig))
                    .add(HumanChromosome.fromString(contig).toString())
                    .add(String.valueOf(1))
                    .add(String.valueOf(Math.max(MIN_KAROTYPE_LENGTH, contigLengths.get(contig))))
                    .add("chr" + HumanChromosome.fromString(contig).toString())
                    .toString();
            result.add(start);
        }

        return result;
    }

    @NotNull
    private List<String> createHistogramTrack(@NotNull final List<Segment> segments)
    {

        final List<String> result = Lists.newArrayList();
        for (final Segment segment : segments)
        {

            if (segment.track() > 0)
            {

                double r0 = CircosConfigWriter.svTrackRelative(maxTracks, segment.track());
                String r0String = "r0=" + r0 + "r";
                double thickness = thicknessPixels(segment.traverseCount());
                String r1String = "r1=" + r0 + "r+" + thickness + "p";

                final String entry = new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                        .add(String.valueOf(segment.start()))
                        .add(String.valueOf(segment.end()))
                        .add(String.valueOf(segment.track()))
                        .add("fill_" + colorPicker.color(segment.clusterId(), segment.chainId()) + "," + r0String + "," + r1String)
                        .toString();
                result.add(entry);

            }
        }

        return result;
    }

    @NotNull
    private Map<String, Integer> contigLengths(@NotNull final List<GenomePosition> positions)
    {
        final Map<String, Integer> results = new LinkedHashMap<>();
        final List<GenomePosition> sortedPositions = positions.stream().sorted().collect(toList());

        for (GenomePosition position : sortedPositions)
        {
            int end = (int) position.position();
            results.merge(position.chromosome(), end, Math::max);
        }
        return results;
    }

    @NotNull
    private List<String> createPositionText(boolean debug, @NotNull final List<Link> originalLinks, @NotNull final List<Link> scaledLinks,
            @NotNull final List<Segment> scaledSegments)
    {

        final Set<String> result = Sets.newHashSet();

        for (int i = 0; i < originalLinks.size(); i++)
        {

            final Link original = originalLinks.get(i);
            final Link scaled = scaledLinks.get(i);

            if (scaled.isValidStart())
            {
                final String start = new StringJoiner(DELIMITER).add(circosContig(scaled.startChromosome()))
                        .add(String.valueOf(scaled.startPosition()))
                        .add(String.valueOf(scaled.startPosition()))
                        .add(String.valueOf(debug ? original.svId() : POSITION_FORMAT.format(original.startPosition())))
                        .toString();

                result.add(start);
            }

            if (scaled.isValidEnd())
            {
                final String start = new StringJoiner(DELIMITER).add(circosContig(scaled.endChromosome()))
                        .add(String.valueOf(scaled.endPosition()))
                        .add(String.valueOf(scaled.endPosition()))
                        .add(String.valueOf(debug ? original.svId() : POSITION_FORMAT.format(original.endPosition())))
                        .toString();

                result.add(start);
            }
        }

        for (final Segment segment : scaledSegments)
        {
            if (segment.startTerminal() != SegmentTerminal.NONE)
            {
                final String startText = segment.startTerminal() == SegmentTerminal.CENTROMERE ? "Centromere" : "Telomere";
                final String start = new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                        .add(String.valueOf(segment.start()))
                        .add(String.valueOf(segment.start()))
                        .add(startText)
                        .toString();
                result.add(start);
            }

            if (segment.endTerminal() != SegmentTerminal.NONE)
            {
                final String endText = segment.endTerminal() == SegmentTerminal.CENTROMERE ? "Centromere" : "Telomere";
                final String start = new StringJoiner(DELIMITER).add(circosContig(segment.chromosome()))
                        .add(String.valueOf(segment.end()))
                        .add(String.valueOf(segment.end()))
                        .add(endText)
                        .toString();
                result.add(start);
            }

        }

        return result.stream().sorted().distinct().collect(toList());
    }

    @NotNull
    private static String circosContig(@NotNull final String chromosome)
    {
        return "hs" + HumanChromosome.fromString(chromosome);
    }

    @NotNull
    private static String thicknessString(long usage)
    {
        return "thickness=" + thicknessPixels(usage);
    }

    static double thicknessPixels(long usage)
    {
        return Math.max(1, 2 + 1.5 * Math.log(usage) / Math.log(2));
    }

    @NotNull
    static String shorthand(long value)
    {
        if (value < 100)
        {
            return String.valueOf(value);
        }

        if (value < 99_950)
        {
            return String.format("%.1fk", value / 1_000d);
        }

        return String.format("%.1fm", value / 1_000_000d);
    }

}