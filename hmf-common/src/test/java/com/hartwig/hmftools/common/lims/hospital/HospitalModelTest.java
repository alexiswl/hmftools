package com.hartwig.hmftools.common.lims.hospital;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class HospitalModelTest {

    @Test
    public void extractPIName() {
        HospitalModel hospitalModel = buildTestHospitalModel();
        assertEquals("Someone", hospitalModel.extractHospitalPI("CPCT01010001"));
        assertEquals("Someone", hospitalModel.extractHospitalPI("DRUP01010001"));
        assertEquals("Someone", hospitalModel.extractHospitalPI("WIDE01010001"));
        assertNull(hospitalModel.extractHospitalPI("CPCT01060001"));
    }

    @Test
    public void extractRequesterName() {
        HospitalModel hospitalModel = buildTestHospitalModel();
        assertEquals("Someone1", hospitalModel.extractRequestName("WIDE01010001", "BB"));
        assertNull(hospitalModel.extractRequestName("DRUP01010001", "BB"));
        assertNull(hospitalModel.extractRequestName("CPCT01010001", "BB"));
        assertEquals("BB", hospitalModel.extractRequestName("CORE01010001", "BB"));
    }

    @Test
    public void extractRequesterEmail() {
        HospitalModel hospitalModel = buildTestHospitalModel();
        assertEquals("my@email.com", hospitalModel.extractRequestEmail("WIDE01010001", "AA"));
        assertNull(hospitalModel.extractRequestEmail("CPCT01010001", "AA"));
        assertNull(hospitalModel.extractRequestEmail("DRUP01010001", "AA"));
        assertEquals("AA", hospitalModel.extractRequestEmail("CORE01010001", "AA"));
    }

    @Test
    public void canLookupAddresseeForSample() {
        HospitalModel hospitalModel = buildTestHospitalModel();
        assertEquals("Ext-HMF, 1000 AB AMSTERDAM", hospitalModel.extractHospitalAddress("CPCT02010001T"));
        assertEquals("Ext-HMF, 1000 AB AMSTERDAM", hospitalModel.extractHospitalAddress("DRUP02010001T"));
        assertEquals("Ext-HMF, 1000 AB AMSTERDAM", hospitalModel.extractHospitalAddress("WIDE02010001T"));
        assertEquals("Ext-HMF, 1000 AB AMSTERDAM", hospitalModel.extractHospitalAddress("CORE02010001T"));
    }

    @Test
    public void canLookupHospitalNameForSample() {
        HospitalModel hospitalModel = buildTestHospitalModel();
        assertEquals("Ext-HMF", hospitalModel.extractHospitalName("CPCT02010001T"));
        assertEquals("Ext-HMF", hospitalModel.extractHospitalName("DRUP02010001T"));
        assertEquals("Ext-HMF", hospitalModel.extractHospitalName("WIDE02010001T"));
        assertEquals("Ext-HMF", hospitalModel.extractHospitalName("CORE02010001T"));
    }

    @NotNull
    public static HospitalModel buildTestHospitalModel() {
        Map<String, HospitalAddress> hospitalAddress = Maps.newHashMap();
        Map<String, HospitalContact> hospitalContactCPCT = Maps.newHashMap();
        Map<String, HospitalContact> hospitalContactDRUP = Maps.newHashMap();
        Map<String, HospitalContact> hospitalContactWIDE = Maps.newHashMap();
        Map<String, HospitalSampleMapping> sampleHospitalMapping = Maps.newHashMap();

        hospitalAddress.put("01", ImmutableHospitalAddress.of("01", "Ext-HMF", "1000 AB", "AMSTERDAM"));
        hospitalContactCPCT.put("01", ImmutableHospitalContact.of("01", "Someone", null, null));
        hospitalContactDRUP.put("01", ImmutableHospitalContact.of("01", "Someone", null, null));
        hospitalContactWIDE.put("01", ImmutableHospitalContact.of("01", "Someone", "Someone1", "my@email.com"));
        sampleHospitalMapping.put("CORE18001224T", ImmutableHospitalSampleMapping.of("01"));

        return ImmutableHospitalModel.builder()
                .hospitalAddress(hospitalAddress)
                .hospitalContactCPCT(hospitalContactCPCT)
                .hospitalContactDRUP(hospitalContactDRUP)
                .hospitalContactWIDE(hospitalContactWIDE)
                .sampleHospitalMapping(sampleHospitalMapping)
                .build();
    }
}