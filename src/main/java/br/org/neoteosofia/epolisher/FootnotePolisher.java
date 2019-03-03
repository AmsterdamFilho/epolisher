package br.org.neoteosofia.epolisher;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Amsterdam Luís
 */
public class FootnotePolisher
{
    private @Inject Epub epub;

    public void start () throws IOException
    {
        FootnoteFileVisitor visitor = new FootnoteFileVisitor();
        Files.walkFileTree(epub.oebps(), visitor);
        int footnoteCount = 0;
        List<Path> sortedPaths = visitor.getSortedPaths();
        for (Path sortedPath : sortedPaths)
        {
            footnoteCount = polishFootnote(sortedPath, footnoteCount);
        }
    }

    private int polishFootnote (Path path, int footNoteCount) throws IOException
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), UTF_8)))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("<div class=\"_idFootnotes\">"))
                {
                    sb.append(inputLine).append("\n");
                    sb.append("\t\t\t\t<aside class=\"_idFootnote\" epub:type=\"footnote\">\n");
                    sb.append("\t\t\t\t\t<p class=\"Epub-texto\">---Notas de rodapé---</p>").append("\n");
                    sb.append("\t\t\t\t</aside>\n");
                }
                else if (inputLine.contains("class=\"Epub-rodape\""))
                {
                    if (inputLine.contains("</a>. "))
                    {
                        sb.append(inputLine.replace("</a>", "</a>" + String.valueOf(++footNoteCount)));
                        sb.append("\n");
                    }
                    else
                    {
                        sb.append(inputLine.replace("</a>", "</a>" + String.valueOf(++footNoteCount) + ". "));
                        sb.append("\n");
                    }
                }
                else
                {
                    sb.append(inputLine).append("\n");
                }
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(path))
        {
            bw.write(sb.toString());
        }
        return footNoteCount;
    }

    private class FootnoteFileVisitor extends SimpleFileVisitor<Path>
    {
        private final List<Path> paths = new ArrayList<>();

        @Override
        public FileVisitResult visitFile (Path path, BasicFileAttributes attrs)
        {
            if (!attrs.isDirectory() && path.toString().toLowerCase().endsWith(".xhtml"))
            {
                paths.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        private List<Path> getSortedPaths ()
        {
            paths.sort(new ChapterFileSorter());
            return paths;
        }
    }

    private class ChapterFileSorter implements Comparator<Path>
    {
        @Override
        public int compare (Path o1, Path o2)
        {
            return Integer.compare(getIndexFromPath(o1), getIndexFromPath(o2));
        }

        private int getIndexFromPath (Path path)
        {
            String name = path.toFile().getName();
            name = name.replace(".xhtml", "");
            if (name.matches(".+-[0-9]+"))
            {
                return Integer.valueOf(name.substring(name.lastIndexOf('-') + 1));
            }
            return 0;
        }
    }
}
