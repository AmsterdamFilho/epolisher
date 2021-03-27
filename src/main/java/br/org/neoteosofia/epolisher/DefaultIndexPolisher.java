package br.org.neoteosofia.epolisher;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static br.org.neoteosofia.epolisher.Epub.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultIndexPolisher implements IndexPolisher {

    private @Inject Epub epub;

    @Override
    public void start() throws IOException {
        DefaultChapterValidator chapterValidator = new DefaultChapterValidator();
        ChapterResolver chapterResolver;
        switch (epub.title()) {
            case CD_I:
                chapterValidator.addAll("PRÓLOGO DO SENHOR", "APÊNDICE");
                chapterResolver = new DefaultChapterResolver();
                break;
            case CD_III:
                chapterValidator.addAll("ANEXO – A FORMAÇÃO");
                chapterResolver = new DefaultChapterResolver();
                break;
            case M:
                chapterValidator.addAll("Prólogo do Senhor");
                chapterResolver = new DefaultChapterResolver();
                break;
            case D:
                return;
            case EXP:
            case MP:
                chapterValidator = new DefaultChapterValidator() {
                    @Override
                    public String regex() {
                        return "idParaDest-([0-9]+)\">– [0-9]+ –";
                    }
                };
                chapterResolver = new DefaultChapterResolver();
                break;
            case GEJ_I:
                chapterValidator.addAll("Prefácio", "Natureza e signi");
                chapterResolver = new DefaultChapterResolver();
                break;
            case GEJ_X:
                chapterValidator.addAll("O GRANDE EVANGELHO", "EPÍLOGO");
                chapterResolver = new GejXChapterResolver();
                break;
            case IJ:
                chapterValidator.addAll("Preâmbulo");
                chapterResolver = new DefaultChapterResolver();
                break;
            case PS:
                chapterValidator.addAll("EPÍLOGO");
                chapterResolver = new DefaultChapterResolver();
                break;
            case RB_II:
                chapterResolver = new DefaultChapterResolver(150);
                break;
            case TL:
                chapterValidator.addAll("Prefácio");
                chapterResolver = new TLChapterResolver();
                break;
            default:
                chapterResolver = new DefaultChapterResolver();
        }
        start(chapterResolver, chapterValidator);
    }

    private void start(ChapterResolver chapterResolver, ChapterValidator chapterValidator) throws IOException {
        String regex = chapterValidator.regex();
        String replacement = "idParaDest\\-$1\">%. ";
        String inputLine;
        StringBuilder sb = new StringBuilder();
        int chapter = 1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(Files.newInputStream(epub.toc()), UTF_8))) {
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("idParaDest") && chapterValidator.validate(inputLine)) {
                    String chapterResolved = chapterResolver.resolve(chapter);
                    sb.append(inputLine.replaceAll(regex, replacement.replace("%", chapterResolved)));
                    sb.append("\n");
                    chapter++;
                } else {
                    sb.append(inputLine).append("\n");
                }
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(epub.toc())) {
            bw.write(sb.toString());
        }
    }

    private interface ChapterResolver {
        String resolve(int chapter);
    }

    private interface ChapterValidator {
        boolean validate(String inputLine);

        default String regex() {
            return "idParaDest-([0-9]+)\">";
        }
    }

    private static class DefaultChapterResolver implements ChapterResolver {
        private final int offset;

        private DefaultChapterResolver() {
            offset = 0;
        }

        private DefaultChapterResolver(int offset) {
            this.offset = offset;
        }

        @Override
        public String resolve(int chapter) {
            return String.valueOf(chapter + offset);
        }
    }

    private static class GejXChapterResolver implements ChapterResolver {
        @Override
        public String resolve(int chapter) {
            if (chapter <= 244) {
                return String.valueOf(chapter);
            } else {
                return String.valueOf((chapter - 244));
            }
        }
    }

    private static class TLChapterResolver implements ChapterResolver {
        @Override
        public String resolve(int chapter) {
            if (chapter < 47) {
                return String.valueOf(chapter);
            } else if (chapter == 47) {
                return "47 e 48";
            } else if (chapter < 67) {
                return String.valueOf((chapter + 1));
            } else if (chapter == 67) {
                return "68 e 69";
            } else if (chapter < 72) {
                return String.valueOf((chapter + 2));
            } else {
                return String.valueOf((chapter - 71));
            }
        }
    }

    private static class DefaultChapterValidator implements ChapterValidator {
        private final List<String> ignoredChapters = new ArrayList<>();

        public DefaultChapterValidator() {
            ignoredChapters.add("Copyright");
        }

        private void addAll(String... ignoredChapters) {
            Collections.addAll(this.ignoredChapters, ignoredChapters);
        }

        @Override
        public boolean validate(String inputLine) {
            return ignoredChapters.stream().noneMatch(inputLine::contains);
        }
    }
}
