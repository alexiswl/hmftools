package com.hartwig.hmftools.patientreporter.cfreport.components;

import com.hartwig.hmftools.common.hospital.HospitalModel;
import com.hartwig.hmftools.common.lims.LimsSampleType;
import com.hartwig.hmftools.patientreporter.PatientReporterApplication;
import com.hartwig.hmftools.patientreporter.SampleReport;
import com.hartwig.hmftools.patientreporter.cfreport.ReportResources;
import com.hartwig.hmftools.patientreporter.cfreport.data.DataUtil;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public final class SidePanel {

    private static final float ROW_SPACING = 42;
    private static final float CONTENT_X_START = 455;
    private static final float RECTANGLE_WIDTH = 170;
    private static final float RECTANGLE_HEIGHT_SHORT = 110;

    public static void renderSidePanel(PdfPage page, @NotNull final SampleReport sampleReport, boolean fullHeight, boolean fullContent) {
        final PdfCanvas canvas = new PdfCanvas(page.getLastContentStream(), page.getResources(), page.getDocument());
        final Rectangle pageSize = page.getPageSize();
        renderBackgroundRect(fullHeight, canvas, pageSize);
        BaseMarker.renderMarkerGrid(4, (fullHeight ? 20 : 2), CONTENT_X_START, 35, 820, -ROW_SPACING, .05f, .15f, canvas);

        int sideTextIndex = 0;
        Canvas cv = new Canvas(canvas, page.getDocument(), page.getPageSize());

        cv.add(createSidePanelDiv(sideTextIndex++, "HMF sample id", sampleReport.sampleId()));
        cv.add(createSidePanelDiv(sideTextIndex++, "Report date", ReportResources.REPORT_DATE));

        LimsSampleType type = LimsSampleType.fromSampleId(sampleReport.sampleId());

        if (fullHeight && fullContent) {
            final String contactNames = type == LimsSampleType.CORE ? sampleReport.requesterName(): sampleReport.hospitalPIName();
            if (!contactNames.isEmpty()) {
                cv.add(createSidePanelDiv(sideTextIndex++, "Name requestor", contactNames));
            }

            final String contactEmails = type == LimsSampleType.CORE ? sampleReport.requesterEmail() : sampleReport.hospitalPIEmail();
            if (!contactEmails.isEmpty()) {
                cv.add(createSidePanelDiv(sideTextIndex++, "Email requestor", contactEmails));
            }

            final String hospitalName = sampleReport.hospitalName(); // @TODO Replace with sampleReport.hospital() which can be null or empty string
            if (hospitalName != null && !hospitalName.isEmpty()) {
                cv.add(createSidePanelDiv(sideTextIndex++, "Hospital", hospitalName));
            }

            final String hospitalPatientId = sampleReport.hospitalPatientId();
            if (hospitalPatientId != null && !hospitalPatientId.isEmpty()) {
                cv.add(createSidePanelDiv(sideTextIndex++, "Hospital patient id", hospitalPatientId));
            }

            // @TODO: Decide add to report
            //            final String patientGender = "Female"; // @TODO Replace with sampleReport.patientGender() which can be null or empty string
//            if (patientGender != null && !patientGender.isEmpty()) {
//                cv.add(createSidePanelDiv(sideTextIndex++, "Gender", patientGender));
//            }
//
            // @TODO: Decide add to report
            //            final LocalDate patientBirthDate =
//                    LocalDate.of(1973, 10, 4); // @TODO Replace with sampleReport.patientBirthDate() which can be null
//            if (patientBirthDate != null) {
//                cv.add(createSidePanelDiv(sideTextIndex, "Birth date", DataUtil.formatDate(patientBirthDate)));
//            }
        }

        if (page.getDocument().getNumberOfPages() == 1) {
            cv.add(new Paragraph(
                    "v" + (PatientReporterApplication.VERSION != null ? PatientReporterApplication.VERSION : "X.X")).setFixedPosition(
                    pageSize.getWidth() - RECTANGLE_WIDTH + 4,
                    40,
                    30).setRotationAngle(Math.PI / 2).setFontColor(ReportResources.PALETTE_LIGHT_GREY).setFontSize(6));
        }

        canvas.release();
    }

    private static void renderBackgroundRect(boolean fullHeight, @NotNull PdfCanvas canvas, @NotNull Rectangle pageSize) {
        canvas.rectangle(pageSize.getWidth(),
                pageSize.getHeight(),
                -RECTANGLE_WIDTH,
                fullHeight ? -pageSize.getHeight() : -RECTANGLE_HEIGHT_SHORT);
        canvas.setFillColor(ReportResources.PALETTE_BLUE);
        canvas.fill();
    }

    @NotNull
    private static Div createSidePanelDiv(int index, @NotNull String label, @NotNull String value) {
        final float Y_START = 802;
        final float VALUE_TEXT_Y_OFFSET = 18;
        final float MAX_WIDTH = 120;

        Div div = new Div();
        div.setKeepTogether(true);

        float yPos = Y_START - index * ROW_SPACING;
        div.add(new Paragraph(label.toUpperCase()).addStyle(ReportResources.sidePanelLabelStyle())
                .setFixedPosition(CONTENT_X_START, yPos, MAX_WIDTH));

        final float valueFontSize = ReportResources.maxPointSizeForWidth(ReportResources.fontBold(), 11, 6, value, MAX_WIDTH);
        yPos -= VALUE_TEXT_Y_OFFSET;
        div.add(new Paragraph(value).addStyle(ReportResources.sidePanelValueStyle().setFontSize(valueFontSize))
                .setHeight(15)
                .setFixedPosition(CONTENT_X_START, yPos, MAX_WIDTH)
                .setFixedLeading(valueFontSize));

        return div;
    }
}
