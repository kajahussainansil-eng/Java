package storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust CSV reader and atomic writer adhering strictly to RFC-4180.
 * Handles commas, quoted text, escaped quotes, and embedded line breaks.
 */
public final class CsvStorage {

    private CsvStorage() {}

    /**
     * Reads all records from a CSV file. Each record is a List of String columns.
     */
    public static List<List<String>> readCsv(File file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        if (!file.exists() || file.length() == 0) {
            return rows;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            StringBuilder currentField = new StringBuilder();
            List<String> currentRow = new ArrayList<>();
            boolean inQuotes = false;
            int chInt;

            while ((chInt = reader.read()) != -1) {
                char c = (char) chInt;

                if (inQuotes) {
                    if (c == '"') {
                        // Check if escaped quote ("")
                        reader.mark(1);
                        int next = reader.read();
                        if (next == '"') {
                            currentField.append('"');
                        } else {
                            inQuotes = false;
                            reader.reset();
                        }
                    } else {
                        currentField.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        currentRow.add(currentField.toString().trim());
                        currentField.setLength(0);
                    } else if (c == '\r') {
                        // Handle \r\n
                        reader.mark(1);
                        int next = reader.read();
                        if (next != '\n') {
                            reader.reset();
                        }
                        currentRow.add(currentField.toString().trim());
                        currentField.setLength(0);
                        if (!currentRow.isEmpty() && !(currentRow.size() == 1 && currentRow.get(0).isEmpty())) {
                            rows.add(new ArrayList<>(currentRow));
                        }
                        currentRow.clear();
                    } else if (c == '\n') {
                        currentRow.add(currentField.toString().trim());
                        currentField.setLength(0);
                        if (!currentRow.isEmpty() && !(currentRow.size() == 1 && currentRow.get(0).isEmpty())) {
                            rows.add(new ArrayList<>(currentRow));
                        }
                        currentRow.clear();
                    } else {
                        currentField.append(c);
                    }
                }
            }

            // Flush remaining field
            if (currentField.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(currentField.toString().trim());
                if (!currentRow.isEmpty() && !(currentRow.size() == 1 && currentRow.get(0).isEmpty())) {
                    rows.add(currentRow);
                }
            }
        }

        return rows;
    }

    /**
     * Atomically writes CSV rows using a temporary file and atomic replace.
     */
    public static void writeCsv(File targetFile, List<String> header, List<List<String>> rows) throws IOException {
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        File tempFile = new File(parentDir, targetFile.getName() + ".tmp");

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            
            if (header != null && !header.isEmpty()) {
                writer.write(formatRow(header));
                writer.newLine();
            }

            for (List<String> row : rows) {
                writer.write(formatRow(row));
                writer.newLine();
            }

            writer.flush();
            fos.getFD().sync(); // Ensure data is committed to disk
        }

        // Atomic move/replace
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String formatRow(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(fields.get(i)));
        }
        return sb.toString();
    }

    public static String escape(String field) {
        if (field == null) return "";
        boolean containsSpecial = field.contains(",") || field.contains("\"") ||
                field.contains("\n") || field.contains("\r");
        if (containsSpecial) {
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }
}
