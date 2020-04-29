package com.hartwig.hmftools.isofox.fusion;

import static java.lang.Math.min;

import static com.hartwig.hmftools.common.utils.Strings.appendStr;
import static com.hartwig.hmftools.common.utils.Strings.appendStrList;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.switchIndex;
import static com.hartwig.hmftools.isofox.common.RegionMatchType.INTRON;
import static com.hartwig.hmftools.isofox.common.RnaUtils.impliedSvType;
import static com.hartwig.hmftools.isofox.common.RnaUtils.positionWithin;
import static com.hartwig.hmftools.isofox.common.TransExonRef.hasTranscriptExonMatch;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.MATCHED_JUNCTION;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.DISCORDANT;
import static com.hartwig.hmftools.isofox.fusion.FusionFragmentType.REALIGNED;
import static com.hartwig.hmftools.isofox.fusion.FusionUtils.formLocationPair;
import static com.hartwig.hmftools.isofox.results.ResultsWriter.DELIMITER;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.ensemblcache.EnsemblGeneData;

import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.variant.structural.StructuralVariantType;
import com.hartwig.hmftools.isofox.common.ReadRecord;
import com.hartwig.hmftools.isofox.common.TransExonRef;

public class FusionReadData
{
    private final int mId;
    private final String mLocationId;
    private final FusionFragment mFragment; // the one which establishes this fusion and whose junction data is used

    private final Map<FusionFragmentType,List<FusionFragment>> mFragments;

    private boolean mIncompleteData;

    private final List<Integer> mRelatedFusions;

    // not stored by stream
    private final String[] mChromosomes;
    private final int[] mGeneCollections;
    private final int[] mJunctionPositions;
    private final byte[] mJunctionOrientations;
    private final List<TransExonRef>[] mTransExonRefs;
    private final int[] mReadDepth;

    // the following data is stored by stream, not start/end
    private final List<EnsemblGeneData>[] mCandidateGenes; // up and downstream genes
    private final String[] mFusionGeneIds;
    private final int[] mStreamIndices; // mapping of up & down stream to position data which is in SV terms

    public static final int FS_UPSTREAM = 0;
    public static final int FS_DOWNSTREAM = 1;
    public static final int FS_PAIR = 2;

    public FusionReadData(int id, final FusionFragment fragment)
    {
        mId = id;
        mFragment = fragment;

        mChromosomes = new String[] { fragment.chromosomes()[SE_START], fragment.chromosomes()[SE_END] };
        mGeneCollections = new int[] { fragment.geneCollections()[SE_START], fragment.geneCollections()[SE_END] };
        mJunctionPositions = new int[] { fragment.junctionPositions()[SE_START], fragment.junctionPositions()[SE_END] };
        mJunctionOrientations = new byte[]{ fragment.junctionOrientations()[SE_START], fragment.junctionOrientations()[SE_END] };

        mFragments = Maps.newHashMap();
        addFusionFragment(fragment);

        mLocationId = formLocationPair(mChromosomes, mGeneCollections);

        mRelatedFusions = Lists.newArrayList();
        mFusionGeneIds = new String[] {"", ""};
        mStreamIndices = new int[] { SE_START, SE_END };
        mReadDepth = new int[] {0, 0};

        mCandidateGenes = new List[FS_PAIR];
        mCandidateGenes[SE_START] = Lists.newArrayList();
        mCandidateGenes[SE_END] = Lists.newArrayList();
        mIncompleteData = false;

        mTransExonRefs = new List[FS_PAIR];
        mTransExonRefs[SE_START] = Lists.newArrayList();
        mTransExonRefs[SE_END] = Lists.newArrayList();
    }

    public int id() { return mId; }
    public String locationId() { return mLocationId; }
    public final String[] chromosomes() { return mChromosomes; }
    public final int[] geneCollections() { return mGeneCollections; }
    public final int[] junctionPositions() { return mJunctionPositions; }
    public final byte[] junctionOrientations() { return mJunctionOrientations; }
    private final String[] junctionBases() { return mFragment.junctionBases(); }

    public boolean hasIncompleteData() { return mIncompleteData; }
    public void setIncompleteData() { mIncompleteData = true; }

    public List<TransExonRef> getTransExonRefsByPos(int se) { return mTransExonRefs[se]; }

    public List<TransExonRef> getTransExonRefsByStream(int fs)
    {
        if(hasViableGenes())
            return mTransExonRefs[mStreamIndices[fs]];

        return mTransExonRefs[fs];
    }

    public final List<FusionFragment> getAllFragments()
    {
        if(mFragments.size() == 1)
            return mFragments.values().iterator().next();

        final List<FusionFragment> fragments = Lists.newArrayList();
        mFragments.values().forEach(x -> fragments.addAll(x));
        return fragments;
    }

    public int fragmentCount() { return mFragments.values().stream().mapToInt(x -> x.size()).sum(); }

    public final Map<FusionFragmentType,List<FusionFragment>> getFragments() { return mFragments; }
    public final List<FusionFragment> getFragments(FusionFragmentType type)
    {
        return mFragments.containsKey(type) ? mFragments.get(type) : Lists.newArrayList();
    }

    public void addFusionFragment(final FusionFragment fragment)
    {
        List<FusionFragment> fragments = mFragments.get(fragment.type());

        if (fragments == null)
        {
            mFragments.put(fragment.type(), Lists.newArrayList(fragment));
            return;
        }

        fragments.add(fragment);
    }

    public boolean hasJunctionFragments() { return mFragments.containsKey(MATCHED_JUNCTION); }

    public boolean isKnownSpliced() { return getSampleFragment().isSpliced(); }
    public boolean isUnspliced() { return getSampleFragment().isUnspliced() && getSampleFragment().type() == MATCHED_JUNCTION; }

    public List<EnsemblGeneData>[] getCandidateGenes() { return mCandidateGenes; }

    public boolean hasViableGenes() { return !mCandidateGenes[FS_UPSTREAM].isEmpty() && !mCandidateGenes[FS_DOWNSTREAM].isEmpty(); }

    public boolean isValid() { return hasViableGenes() && !hasIncompleteData(); }

    public void setStreamData(final List<EnsemblGeneData> upstreamGenes, final List<EnsemblGeneData> downstreamGenes, boolean startIsUpstream)
    {
        mStreamIndices[FS_UPSTREAM] = startIsUpstream ? SE_START : SE_END;
        mStreamIndices[FS_DOWNSTREAM] = startIsUpstream ? SE_END : SE_START;
        mCandidateGenes[FS_UPSTREAM] = upstreamGenes;
        mCandidateGenes[FS_DOWNSTREAM] = downstreamGenes;

        // until a more informed decision can be made
        mFusionGeneIds[FS_UPSTREAM] = upstreamGenes.get(0).GeneId;
        mFusionGeneIds[FS_DOWNSTREAM] = downstreamGenes.get(0).GeneId;
    }

    public byte[] getGeneStrands()
    {
        if(!hasViableGenes())
            return null;

        if(mStreamIndices[FS_UPSTREAM] == SE_START)
            return new byte[] { mCandidateGenes[SE_START].get(0).Strand, mCandidateGenes[SE_END].get(0).Strand };
        else
            return new byte[] { mCandidateGenes[SE_END].get(0).Strand, mCandidateGenes[SE_START].get(0).Strand };
    }

    public final List<Integer> getRelatedFusions() { return mRelatedFusions; }

    public void addRelatedFusion(int id)
    {
        if(!mRelatedFusions.contains(id))
            mRelatedFusions.add(id);
    }

    public StructuralVariantType getImpliedSvType()
    {
        return impliedSvType(mChromosomes, mJunctionOrientations);
    }

    public boolean junctionMatch(final FusionFragment fragment)
    {
        return fragment.type() == MATCHED_JUNCTION
                && mJunctionPositions[SE_START] == fragment.junctionPositions()[SE_START] && mJunctionPositions[SE_END] == fragment.junctionPositions()[SE_END]
                && mJunctionOrientations[SE_START] == fragment.junctionOrientations()[SE_START] && mJunctionOrientations[SE_END] == fragment.junctionOrientations()[SE_END];
    }

    public FusionFragment getSampleFragment()
    {
        if(mFragments.containsKey(MATCHED_JUNCTION))
        {
            return mFragments.get(MATCHED_JUNCTION).get(0);
        }
        else
        {
            return mFragments.values().iterator().next().get(0);
        }
    }

    public void cacheTranscriptData()
    {
        // select a sample fragment from which to extract transcript-exon data
        final FusionFragment sampleFragment = getSampleFragment();

        for (int se = SE_START; se <= SE_END; ++se)
        {
            mTransExonRefs[se].addAll(sampleFragment.getTransExonRefs()[se]);
        }
    }

    public boolean canAddUnfusedFragment(final FusionFragment fragment, int maxFragmentDistance)
    {
        // a discordant read spans both genes and cannot be outside the standard long fragment length either in intronic terms
        // or by exons if exonic

        // a realigned fragment must touch one of the fusion junctions with soft-clipping

        // the 2 reads' bounds need to fall within 2 or less exons away
        // apply max fragment distance criteria

        int impliedFragmentLength = fragment.getReads().get(0).Length * 2;

        for(int se = SE_START; se <= SE_END; ++se)
        {
            final List<TransExonRef> fragmentRefs = fragment.getTransExonRefs()[se];
            final List<TransExonRef> fusionRefs = getTransExonRefsByPos(se);

            // must match the orientations of the fusion junction
            if(fragment.orientations()[se] != mJunctionOrientations[se])
                continue;

            boolean isUpstream = (mStreamIndices[FS_UPSTREAM] == se);

            int permittedExonDiff;

            if(isUpstream)
                permittedExonDiff = -2;
            else if(fragment.regionMatchTypes()[se] == INTRON)
                permittedExonDiff = 1;
            else
                permittedExonDiff = 2;

            if(!hasTranscriptExonMatch(fusionRefs, fragmentRefs, permittedExonDiff))
                return false;

            final int seIndex = se;
            final ReadRecord read = fragment.getReads().stream()
                .filter(x -> x.Chromosome.equals(mChromosomes[seIndex]) && x.getGeneCollecton() == mGeneCollections[seIndex])
                    .findFirst().orElse(null);

            if(read == null)
                return false;

            int fragmentPosition = read.getCoordsBoundary(switchIndex(se));

            // cannot be on the wrong side of the junction
            if((mJunctionOrientations[se] == 1) == (fragmentPosition > mJunctionPositions[se]))
            {
                // check if a mis-mapping explains the over-hang
                if(!softClippedReadSupportsJunction(read, se))
                    return false;
            }

            if(fragment.regionMatchTypes()[se] == INTRON)
            {
                if (fragmentPosition < mJunctionPositions[se])
                    impliedFragmentLength += mJunctionPositions[se] - fragmentPosition;
                else
                    impliedFragmentLength += fragmentPosition - mJunctionPositions[se];
            }
        }

        if(impliedFragmentLength > maxFragmentDistance)
            return false;

        return true;
    }

    public boolean isRelignedFragment(final FusionFragment fragment)
    {
        for (int se = SE_START; se <= SE_END; ++se)
        {
            for (ReadRecord read : fragment.getReads())
            {
                if (!read.Chromosome.equals(mChromosomes[se]))
                    continue;

                if (softClippedReadSupportsJunction(read, se))
                {
                    if(fragment.geneCollections()[se] == 0 && fragment.getTransExonRefs()[se].isEmpty())
                        fragment.setGeneData(mGeneCollections[se], getTransExonRefsByPos(se));

                    return true;
                }
            }
        }

        return false;
    }

    private final static int SOFT_CLIP_JUNC_BUFFER = 3;
    private final static int SOFT_CLIP_MIN_BASE_MATCH = 3;
    private final static int SOFT_CLIP_MAX_BASE_MATCH = 5;

    private boolean softClippedReadSupportsJunction(final ReadRecord read, int juncSeIndex)
    {
        // compare a minimum number of soft-clipped bases to the other side of the exon junction
        // if the read extends past break junction, include these bases in what is compared against the next junction to account for homology
        if(mJunctionOrientations[juncSeIndex] == 1)
        {
            if(!read.isSoftClipped(SE_END))
                return false;

            int readBoundary = read.getCoordsBoundary(SE_END);

            if(!positionWithin(readBoundary, mJunctionPositions[juncSeIndex], mJunctionPositions[juncSeIndex] + SOFT_CLIP_JUNC_BUFFER))
                return false;

            // test that soft-clipped bases match the other junction's bases
            int scLength = read.Cigar.getLastCigarElement().getLength();

            if(scLength < SOFT_CLIP_MIN_BASE_MATCH)
                return false;

            // if the junction is 1 base higher, then take 1 base off the soft-clipped bases
            int posAdjust = readBoundary > mJunctionPositions[juncSeIndex] ? readBoundary - mJunctionPositions[juncSeIndex] : 0;

            String extraBases = read.ReadBases.substring(read.Length - scLength - posAdjust, read.Length);

            if(extraBases.length() > SOFT_CLIP_MAX_BASE_MATCH)
                extraBases = extraBases.substring(0, SOFT_CLIP_MAX_BASE_MATCH);

            return junctionBases()[switchIndex(juncSeIndex)].startsWith(extraBases);
        }
        else
        {
            if(!read.isSoftClipped(SE_START))
                return false;

            int readBoundary = read.getCoordsBoundary(SE_START);

            if(!positionWithin(readBoundary, mJunctionPositions[juncSeIndex] - SOFT_CLIP_JUNC_BUFFER, mJunctionPositions[juncSeIndex]))
                return false;

            int scLength = read.Cigar.getFirstCigarElement().getLength();

            if(scLength < SOFT_CLIP_MIN_BASE_MATCH)
                return false;

            int posAdjust = readBoundary < mJunctionPositions[juncSeIndex] ? mJunctionPositions[juncSeIndex] - readBoundary : 0;

            String extraBases = read.ReadBases.substring(0, scLength + posAdjust);

            if(extraBases.length() > SOFT_CLIP_MAX_BASE_MATCH)
                extraBases = extraBases.substring(extraBases.length() - SOFT_CLIP_MAX_BASE_MATCH, extraBases.length());

            return junctionBases()[switchIndex(juncSeIndex)].endsWith(extraBases);
        }
    }

    public int[] getReadDepth() { return mReadDepth; }

    public String getGeneName(int stream)
    {
        if(mCandidateGenes[stream].isEmpty())
            return "";

        if(mFusionGeneIds[stream].isEmpty())
            return mCandidateGenes[stream].get(0).GeneName;

        return mCandidateGenes[stream].stream()
                .filter(x -> x.GeneId.equals(mFusionGeneIds[stream])).findFirst().map(x -> x.GeneName).orElse("");
    }

    public String toString()
    {
        return String.format("%d: chr(%s-%s) junc(%d-%d %d/%d %s) genes(%s-%s) frags(%d)",
                mId, mChromosomes[SE_START], mChromosomes[SE_END], mJunctionPositions[SE_START], mJunctionPositions[SE_END],
                mJunctionOrientations[SE_START], mJunctionOrientations[SE_END], getImpliedSvType(),
                getGeneName(FS_UPSTREAM), getGeneName(FS_DOWNSTREAM), mFragments.size());
    }

    public static String csvHeader()
    {
        return "FusionId,Valid,GeneIdUp,GeneNameUp,ChrUp,PosUp,OrientUp,StrandUp,JuncTypeUp"
                + ",GeneIdDown,GeneNameDown,ChrDown,PosDown,OrientDown,StrandDown,JuncTypeDown"
                + ",SVType,TotalFragments,SplitFrags,RealignedFrags,DiscordantFrags,JuncDepthUp,JuncDepthDown"
                + ",TransDataUp,TransDataDown,OtherGenesUp,OtherGenesDown,RelatedFusions";
    }

    public static String fusionId(int id) { return String.format("Id_%d", id); }

    public String toCsv()
    {
        StringJoiner csvData = new StringJoiner(DELIMITER);

        csvData.add(fusionId(mId));
        csvData.add(String.valueOf(hasViableGenes() && !hasIncompleteData()));

        final FusionFragment sampleFragment = getSampleFragment();

        for(int fs = FS_UPSTREAM; fs <= FS_DOWNSTREAM; ++fs)
        {
            final String geneId = mFusionGeneIds[fs];
            final List<EnsemblGeneData> genes = mCandidateGenes[fs];

            csvData.add(geneId);

            final EnsemblGeneData geneData = genes.stream()
                    .filter(x -> x.GeneId.equals(geneId)).findFirst().map(x -> x).orElse(null);

            if(geneData != null)
            {
                csvData.add(geneData.GeneName);

                csvData.add(mChromosomes[mStreamIndices[fs]]);
                csvData.add(String.valueOf(mJunctionPositions[mStreamIndices[fs]]));
                csvData.add(String.valueOf(mJunctionOrientations[mStreamIndices[fs]]));
                csvData.add(String.valueOf(geneData.Strand));
                csvData.add(sampleFragment.junctionTypes()[mStreamIndices[fs]].toString());
            }
            else
            {
                csvData.add("");
                csvData.add(mChromosomes[fs]);
                csvData.add(String.valueOf(mJunctionPositions[fs]));
                csvData.add(String.valueOf(mJunctionOrientations[fs]));
                csvData.add("0");
                csvData.add(FusionJunctionType.UNKNOWN.toString());
            }
        }

        csvData.add(getImpliedSvType().toString());

        int splitFragments = mFragments.containsKey(MATCHED_JUNCTION) ? mFragments.get(MATCHED_JUNCTION).size() : 0;
        int realignedFragments = mFragments.containsKey(REALIGNED) ? mFragments.get(REALIGNED).size() : 0;
        int discordantFragments = mFragments.containsKey(DISCORDANT) ? mFragments.get(DISCORDANT).size() : 0;

        int totalFragments = splitFragments + realignedFragments + discordantFragments;

        csvData.add(String.valueOf(totalFragments));
        csvData.add(String.valueOf(splitFragments));
        csvData.add(String.valueOf(realignedFragments));
        csvData.add(String.valueOf(discordantFragments));
        csvData.add(String.valueOf(mReadDepth[mStreamIndices[FS_UPSTREAM]]));
        csvData.add(String.valueOf(mReadDepth[mStreamIndices[FS_DOWNSTREAM]]));

        for (int fs = FS_UPSTREAM; fs <= FS_DOWNSTREAM; ++fs)
        {
            final List<TransExonRef> transExonRefs = getTransExonRefsByStream(fs);
            if(transExonRefs.isEmpty())
            {
                csvData.add("NONE");
                continue;
            }

            String transData = "";
            for(final TransExonRef transExonRef : transExonRefs)
            {
                transData = appendStr(transData, String.format("%s-%d", transExonRef.TransName, transExonRef.ExonRank), ';');
            }

            csvData.add(transData);
        }

        if(hasViableGenes())
        {
            String[] otherGenes = new String[] {"", ""};

            for (int fs = FS_UPSTREAM; fs <= FS_DOWNSTREAM; ++fs)
            {
                for (final EnsemblGeneData geneData : mCandidateGenes[fs])
                {
                    if (!geneData.GeneId.equals(mFusionGeneIds[fs]))
                    {
                        otherGenes[fs] = appendStr(otherGenes[fs], geneData.GeneName, ';');
                    }
                }

                csvData.add(!otherGenes[fs].isEmpty() ? otherGenes[fs] : "NONE");
            }
        }
        else
        {
            csvData.add("NONE");
            csvData.add("NONE");
        }

        if(!mRelatedFusions.isEmpty())
        {
            List<String> relatedFusions = mRelatedFusions.stream().map(x -> fusionId(x)).collect(Collectors.toList());
            csvData.add(appendStrList(relatedFusions, ';'));
        }
        else
        {
            csvData.add("NONE");
        }

        return csvData.toString();
    }

}
