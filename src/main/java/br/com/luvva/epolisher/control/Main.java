package br.com.luvva.epolisher.control;

import javax.swing.*;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Lima Filho, A. L. - amsterdam@luvva.com.br
 */
public class Main
{

    public static void main (String[] args)
    {
        Path oebpsPath = getOEBPSPath(args);
        if (oebpsPath != null)
        {
            File tocFile = getTocFile(oebpsPath);
            if (tocFile != null)
            {
                try
                {
                    addChapterNumbers(tocFile);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(null, "Error editing toc file: " + e.getMessage());
                }
            }
            try
            {
                FootnoteFileVisitor visitor = new FootnoteFileVisitor();
                Files.walkFileTree(oebpsPath, visitor);
                int footnoteCount = 0;
                List<Path> sortedPaths = visitor.getSortedPaths();
                for (Path sortedPath : sortedPaths)
                {
                    footnoteCount = polishFootnote(sortedPath, footnoteCount);
                }
            }
            catch (IOException e)
            {
                JOptionPane.showMessageDialog(null, "Error editing footnotes: " + e.getMessage());
            }
        }
    }

    private static Path getOEBPSPath (String[] args)
    {
        if (args.length == 0)
        {
            JOptionPane.showMessageDialog(null, "Especify Epub directory for polishment!");
        }
        else
        {
            File epubDirectory = new File(args[0]);
            if (!epubDirectory.exists())
            {
                JOptionPane.showMessageDialog(null, "Especified Epub directory does not exist!");
            }
            else if (!epubDirectory.isDirectory())
            {
                JOptionPane.showMessageDialog(null, "Choose Epub directory, not a file!");
            }
            else
            {
                Path oebps = epubDirectory.toPath().resolve("OEBPS");
                File oebpsFile = oebps.toFile();
                if (!(oebpsFile.exists() && oebpsFile.isDirectory()))
                {
                    JOptionPane.showMessageDialog(null, "OEBPS directory could not be resolved!");
                }
                else
                {
                    return oebps;
                }
            }
        }
        return null;
    }

    private static File getTocFile (Path oebps)
    {
        File tocFile = oebps.resolve("toc.xhtml").toFile();
        if (!(tocFile.exists() && tocFile.isFile()))
        {
            JOptionPane.showMessageDialog(null, "toc.xhtml file could not be resolved!");
        }
        else
        {
            return tocFile;
        }
        return null;
    }

    private static void addChapterNumbers (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest"))
                {
                    sb.append(
                            inputLine.replaceAll("idParaDest\\-([0-9]+)\">", "idParaDest\\-$1\">$1. "))
                      .append("\n");
                }
                else
                {
                    sb.append(inputLine).append("\n");
                }
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(tocFile.toPath()))
        {
            bw.write(sb.toString());
        }
    }

    private static class FootnoteFileVisitor extends SimpleFileVisitor<Path>
    {

        private final List<Path> paths = new ArrayList<>();

        @Override
        public FileVisitResult visitFile (Path path, BasicFileAttributes attrs) throws IOException
        {
            if (!attrs.isDirectory() && path.toString().toLowerCase().endsWith(".xhtml"))
            {
                paths.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        private List<Path> getSortedPaths ()
        {
            Collections.sort(paths, new ChapterFileSorter());
            return paths;
        }
    }

    private static class ChapterFileSorter implements Comparator<Path>
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
            if (name.matches(".+\\-[0-9]+"))
            {
                return Integer.valueOf(name.substring(name.lastIndexOf('-') + 1));
            }
            return 0;
        }

    }

    private static int polishFootnote (Path path, int footNoteCount) throws IOException
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("<div class=\"_idFootnotes\">"))
                {
                    sb.append(inputLine).append("\n");
                    sb.append("\t\t\t\t<aside class=\"_idFootnote\" epub:type=\"footnote\">\n");
                    sb.append("\t\t\t\t\t<p class=\"Texto\">---Notas de rodapé---</p>").append("\n");
                    sb.append("\t\t\t\t</aside>\n");
                }
                else if (inputLine.contains("class=\"Rodape\""))
                {
                    sb.append(inputLine.replace("</a>", "</a>" + String.valueOf(++footNoteCount) + ". ")).append("\n");
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

}
