package edu.itc.salesreport.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void rendersAllBranchesAndChainTotal(String ext) {
        ReportRenderer renderer = ReportRenderer.forExtension(ext);
        assertEquals(ext, renderer.extension());

        String output = new String(renderer.render(TestReports.september()), StandardCharsets.UTF_8);
        assertTrue(output.contains("PNH"));
        assertTrue(output.contains("REP"));
        assertTrue(output.contains("BTB"));
        assertTrue(output.contains("ALL"));
        assertTrue(output.contains("2300.50"));
    }

    @Test
    void csvRendererProducesExactExpectedLine() {
        ReportRenderer renderer = ReportRenderer.forExtension("csv");
        String output = new String(renderer.render(TestReports.september()), StandardCharsets.UTF_8);
        assertTrue(output.contains("month,branch,revenue,discounts,receipts,average_basket\n"));
        assertTrue(output.contains("2026-09,REP,800.50,0.00,50,16.01\n"));
    }

    @Test
    void htmlRendererEscapesSpecialCharacters() {
        var report = TestReports.september();
        String html = new String(new HtmlReportRenderer().render(report), StandardCharsets.UTF_8);
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<tr><td>REP</td><td>800.50</td><td>50</td><td>16.01</td></tr>"));
    }

    @Test
    void unknownExtensionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ReportRenderer.forExtension("pdf"));
    }

    @Test
    void exhaustiveSwitchOverSealedRendererWithoutDefault() {
        ReportRenderer renderer = ReportRenderer.forExtension("html");
        String kind = switch (renderer) {
            case TextReportRenderer t -> "plain:" + t.extension();
            case HtmlReportRenderer h -> "web:" + h.extension();
            case CsvReportRenderer c -> "table:" + c.extension();
        };
        assertEquals("web:html", kind);
    }
}