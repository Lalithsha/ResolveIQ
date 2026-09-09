package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.CsvSanitizationPort;
import com.resolveiq.analysis.domain.model.evidence.ArtifactType;
import com.resolveiq.analysis.domain.model.evidence.ObservationType;
import com.resolveiq.analysis.domain.model.evidence.RedactionCategory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicCsvSanitizationAdapter implements CsvSanitizationPort {

    private static final int MAX_ROWS = 10_000;
    private static final int MAX_COLS = 100;

    @Override
    public ExtractionResult sanitizeCsv(String fileName, byte[] content) {
        String raw = new String(content, StandardCharsets.UTF_8);
        String[] lines = raw.split("\r?\n");
        StringBuilder sanitized = new StringBuilder();
        List<ObservationData> observations = new ArrayList<>();
        List<RedactionData> redactions = new ArrayList<>();

        int rowCount = Math.min(lines.length, MAX_ROWS);
        int colCount = 0;

        for (int r = 0; r < rowCount; r++) {
            String line = lines[r];
            String[] cells = line.split(",", -1);
            if (r == 0) {
                colCount = Math.min(cells.length, MAX_COLS);
            }
            StringBuilder rowBuilder = new StringBuilder();
            int limitCols = Math.min(cells.length, MAX_COLS);
            for (int c = 0; c < limitCols; c++) {
                String cell = cells[c];
                // Neutralize spreadsheet formula injection characters: =, +, -, @
                if (!cell.isEmpty() && (cell.startsWith("=") || cell.startsWith("+") || cell.startsWith("-") || cell.startsWith("@"))) {
                    redactions.add(new RedactionData(RedactionCategory.SECRET_TOKEN, "row=" + r + ",col=" + c, "FORMULA_ESCAPE"));
                    cell = "'" + cell;
                }
                if (c > 0) rowBuilder.append(",");
                rowBuilder.append(cell);
            }
            if (r > 0) sanitized.append("\n");
            sanitized.append(rowBuilder);
        }

        observations.add(new ObservationData(
            ObservationType.INVOICE_FACT,
            "CSV_SCHEMA",
            "CSV schema validated: " + rowCount + " rows, " + colCount + " columns. Formula characters sanitized.",
            1.0,
            "{\"rows\": " + rowCount + ", \"columns\": " + colCount + "}"
        ));

        return new ExtractionResult(
            ArtifactType.CSV_SANITIZED,
            sanitized.toString(),
            observations,
            redactions,
            rowCount,
            null
        );
    }
}
