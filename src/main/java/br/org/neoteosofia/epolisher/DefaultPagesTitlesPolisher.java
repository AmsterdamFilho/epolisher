package br.org.neoteosofia.epolisher;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DefaultPagesTitlesPolisher implements PagesTitlesPolisher {

    private @Inject Epub epub;

    @Override
    public void start() throws IOException {
        switch (epub.title()) {
            case Epub.EXP:
                start("EXP-epub");
                break;
            case Epub.MP:
                start("MP-epub");
                break;
        }
    }

    private void start(String id) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(epub.oebps())) {
            for (Path chapterFile : stream) {
                if (chapterFile.getFileName().toString().startsWith(id)) {
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    FileInputStream fis = new FileInputStream(chapterFile.toFile());
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(fis, UTF_8))) {
                        while ((inputLine = in.readLine()) != null) {
                            if (inputLine.contains("Epub-TitCapIndice")) {
                                StringBuilder sb1 = new StringBuilder();
                                boolean reachedATag = false;
                                boolean reachedSecondTrace = false;
                                boolean reachedFirstTrace = false;
                                for (int i = 0; i < inputLine.length(); i++) {
                                    String currentChar = inputLine.substring(i, i + 1);
                                    if (reachedATag && Objects.equals("/", currentChar)) {
                                        sb1.append("/a></p>");
                                        break;
                                    }
                                    if (reachedATag && !Objects.equals("/", currentChar)) {
                                        sb1.append(currentChar);
                                        continue;
                                    }
                                    if (reachedSecondTrace && Objects.equals("<", currentChar)) {
                                        sb1.append(currentChar);
                                        reachedATag = true;
                                        continue;
                                    }
                                    if (reachedSecondTrace && !Objects.equals("<", currentChar)) {
                                        continue;
                                    }
                                    if (Objects.equals("–", currentChar)) {
                                        if (reachedFirstTrace) {
                                            reachedSecondTrace = true;
                                            sb1.append(currentChar);
                                        } else {
                                            reachedFirstTrace = true;
                                            sb1.append(currentChar);
                                        }
                                    } else {
                                        sb1.append(currentChar);
                                    }
                                }
                                sb.append(sb1).append("\n");
                            } else {
                                sb.append(inputLine).append("\n");
                            }
                        }
                    }
                    try (BufferedWriter bw = Files.newBufferedWriter(chapterFile)) {
                        bw.write(sb.toString());
                    }
                }
            }
        }
    }
}
