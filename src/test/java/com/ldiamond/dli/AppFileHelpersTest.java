package com.ldiamond.dli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppFileHelpersTest {

    private final App app = new App();

    @TempDir
    Path tempDir;

    @Nested
    class ReadDirectoryTreeToString {

        @Test
        void readsJavaFilesInTopLevelDirectory() throws IOException {
            Files.write(tempDir.resolve("A.java"), List.of("class A {}"));

            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".java")).isEqualTo("class A {}");
        }

        @Test
        void recursesIntoNestedSubdirectories() throws IOException {
            Path nested = Files.createDirectories(tempDir.resolve("a/b/c"));
            Files.write(tempDir.resolve("Top.java"), List.of("TOP"));
            Files.write(nested.resolve("Deep.java"), List.of("DEEP"));

            String result = app.readDirectoryTreeToString(tempDir.toString(), ".java");

            assertThat(result).contains("TOP").contains("DEEP").hasSize("TOP".length() + "DEEP".length());
        }

        @Test
        void skipsFilesWithOtherExtensions() throws IOException {
            Files.write(tempDir.resolve("Keep.java"), List.of("KEEP"));
            Files.write(tempDir.resolve("notes.txt"), List.of("SKIP_TXT"));
            Files.write(tempDir.resolve("Keep.java.bak"), List.of("SKIP_BAK"));

            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".java")).isEqualTo("KEEP");
        }

        @Test
        void joinsLinesWithoutSeparator() throws IOException {
            // Current behavior: line breaks are dropped when files are concatenated.
            Files.write(tempDir.resolve("Multi.java"), List.of("line1", "line2", "line3"));

            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".java")).isEqualTo("line1line2line3");
        }

        @Test
        void emptyDirectoryReturnsEmptyString() throws IOException {
            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".java")).isEmpty();
        }

        @Test
        void directoryWithOnlySubdirectoriesReturnsEmptyString() throws IOException {
            Files.createDirectories(tempDir.resolve("x/y"));

            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".java")).isEmpty();
        }

        @Test
        void missingDirectoryThrows() {
            String missing = tempDir.resolve("does-not-exist").toString();

            assertThatThrownBy(() -> app.readDirectoryTreeToString(missing, ".java"))
                .isInstanceOf(NoSuchFileException.class);
        }

        @Test
        @Disabled("Bug: the extension parameter is ignored; \".java\" is hardcoded in App.readDirectoryTreeToString")
        void honorsExtensionParameter() throws IOException {
            Files.write(tempDir.resolve("notes.txt"), List.of("TXT"));
            Files.write(tempDir.resolve("A.java"), List.of("JAVA"));

            assertThat(app.readDirectoryTreeToString(tempDir.toString(), ".txt")).isEqualTo("TXT");
        }
    }

    @Nested
    class PdfExtraction {

        private static final String PDF_TEXT = "Hello Code Reviewer";

        private Path createPdf(final String text) throws IOException {
            Path pdf = tempDir.resolve("sample.pdf");
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(text);
                    content.endText();
                }
                document.save(pdf.toFile());
            }
            return pdf;
        }

        @Test
        void getTextFromPdfExtractsText() throws IOException {
            Path pdf = createPdf(PDF_TEXT);

            assertThat(app.getTextFromPDF(pdf.toString())).contains(PDF_TEXT);
        }

        @Test
        void getTextFromPdfReturnsEmptyForMissingFile() {
            assertThat(app.getTextFromPDF(tempDir.resolve("missing.pdf").toString())).isEmpty();
        }

        @Test
        void getTextFromPdfReturnsEmptyForNonPdfFile() throws IOException {
            Path notPdf = Files.writeString(tempDir.resolve("fake.pdf"), "this is not a pdf");

            assertThat(app.getTextFromPDF(notPdf.toString())).isEmpty();
        }

        @Test
        void getHtmlFromPdfWrapsTextInHtml() throws IOException {
            Path pdf = createPdf(PDF_TEXT);

            String html = app.getHtmlFromPDF(pdf.toString());

            assertThat(html.toLowerCase()).contains("<html").contains("</html>");
            assertThat(html).contains(PDF_TEXT);
        }

        @Test
        void getHtmlFromPdfReturnsEmptyForMissingFile() {
            assertThat(app.getHtmlFromPDF(tempDir.resolve("missing.pdf").toString())).isEmpty();
        }

        @Test
        void getHtmlFromPdfReturnsEmptyForNonPdfFile() throws IOException {
            Path notPdf = Files.writeString(tempDir.resolve("fake.pdf"), "this is not a pdf");

            assertThat(app.getHtmlFromPDF(notPdf.toString())).isEmpty();
        }
    }
}
