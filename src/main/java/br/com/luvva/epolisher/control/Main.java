package br.com.luvva.epolisher.control;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * @author Lima Filho, A. L. - amsterdam@luvva.com.br
 */
public class Main
{
    private static final String CD_I   = "A Criação de Deus (Volume I)";
    private static final String CD_III = "A Criação de Deus (Volume III)";
    private static final String IJ     = "A Infância de Jesus";
    private static final String MP     = "Mensagens do Pai";
    private static final String EXP    = "Explicações de Textos das Escrituras Sagradas";
    private static final String RB_II  = "Roberto Blum (Volume II)";

    public static void main (String[] args)
    {
        Path oebpsPath = getOEBPSPath(args);
        if (oebpsPath != null)
        {
            File tocFile = getTocFile(oebpsPath);
            if (tocFile != null)
            {
                String bookTitle;
                try
                {
                    bookTitle = getBookTitle(tocFile);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(null, "Error getting bookTitle name: " + e.getMessage());
                    return;
                }
                try
                {
                    switch (bookTitle)
                    {
                        case CD_I:
                            addChapterNumbersCD_I(tocFile);
                            break;
                        case CD_III:
                            addChapterNumbersCD_III(tocFile);
                            break;
                        case EXP:
                            fixIndex(tocFile);
                            fixAllPageTitles(oebpsPath, "EXP-epub");
                            break;
                        case IJ:
                            addChapterNumbersInfancia(tocFile);
                            break;
                        case MP:
                            fixIndex(tocFile);
                            fixAllPageTitles(oebpsPath, "MP-epub");
                            break;
                        case RB_II:
                            addChapterNumbersBlumII(tocFile);
                            break;
                        default:
                            addChapterNumbers(tocFile);
                    }
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

    private static void fixAllPageTitles (Path oebpsPath, String id)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(oebpsPath))
        {
            for (Path chapterFile : stream)
            {
                if (chapterFile.getFileName().toString().startsWith(id))
                {
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    FileInputStream fis = new FileInputStream(chapterFile.toFile());
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(fis, "UTF8")))
                    {
                        while ((inputLine = in.readLine()) != null)
                        {
                            if (inputLine.contains("Epub-TitCapIndice"))
                            {
                                StringBuilder sb1 = new StringBuilder();
                                boolean reachedATag = false;
                                boolean reachedSecondTrace = false;
                                boolean reachedFirstTrace = false;
                                for (int i = 0; i < inputLine.length(); i++)
                                {
                                    String currentChar = inputLine.substring(i, i + 1);
                                    if (reachedATag && Objects.equals("/", currentChar))
                                    {
                                        sb1.append("/a></p>");
                                        break;
                                    }
                                    if (reachedATag && !Objects.equals("/", currentChar))
                                    {
                                        sb1.append(currentChar);
                                        continue;
                                    }
                                    if (reachedSecondTrace && Objects.equals("<", currentChar))
                                    {
                                        sb1.append(currentChar);
                                        reachedATag = true;
                                        continue;
                                    }
                                    if (reachedSecondTrace && !Objects.equals("<", currentChar))
                                    {
                                        continue;
                                    }
                                    if (Objects.equals("–", currentChar))
                                    {
                                        if (reachedFirstTrace)
                                        {
                                            reachedSecondTrace = true;
                                            sb1.append(currentChar);
                                        }
                                        else
                                        {
                                            reachedFirstTrace = true;
                                            sb1.append(currentChar);
                                        }
                                    }
                                    else
                                    {
                                        sb1.append(currentChar);
                                    }
                                }
                                sb.append(sb1).append("\n");
                            }
                            else
                            {
                                sb.append(inputLine).append("\n");
                            }
                        }
                    }
                    try (BufferedWriter bw = Files.newBufferedWriter(chapterFile))
                    {
                        bw.write(sb.toString());
                    }
                }
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private static String getBookTitle (File tocFile) throws Exception
    {
        String inputLine;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.matches("\t\t<title>.+</title>"))
                {
                    return inputLine.replace("</title>", "").replace("<title>", "").replace("\t", "");
                }
            }
        }
        throw new NullPointerException("Could not find book's title!");
    }

    private static Path getOEBPSPath (String[] args)
    {
        if (args.length == 0)
        {
            JOptionPane.showMessageDialog(null, "Specify Epub directory for polishment!");
        }
        else
        {
            File epubDirectory = new File(args[0]);
            if (!epubDirectory.exists())
            {
                JOptionPane.showMessageDialog(null, "Specified Epub directory does not exist!");
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

    private static void addChapterNumbersCD_I (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        int cap = 1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest") && !inputLine.contains("PRÓLOGO DO SENHOR") && !inputLine
                        .contains("APÊNDICE"))
                {
                    sb.append(inputLine.replaceAll("idParaDest-([0-9]+)\">", "idParaDest\\-$1\">" + String.valueOf
                            (cap++) + ". "))
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

    private static void addChapterNumbersCD_III (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        int cap = 1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest") && !inputLine.contains("ANEXO – A FORMAÇÃO"))
                {
                    sb.append(inputLine.replaceAll("idParaDest-([0-9]+)\">", "idParaDest\\-$1\">" + String.valueOf
                            (cap++) + ". "))
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

    private static void fixIndex (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest"))
                {
                    sb.append(inputLine.replaceAll(">– ([0-9]+) –", ">$1. ")).append("\n");
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

    private static void addChapterNumbersInfancia (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        int cap = 1;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest") && !inputLine.contains("Preâmbulo"))
                {
                    sb.append(inputLine.replaceAll("idParaDest-([0-9]+)\">", "idParaDest\\-$1\">" + String.valueOf
                            (cap++) + ". "))
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

    private static void addChapterNumbersBlumII (File tocFile) throws Exception
    {
        String inputLine;
        StringBuilder sb = new StringBuilder();
        int cap = 151;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tocFile), "UTF8")))
        {
            while ((inputLine = in.readLine()) != null)
            {
                if (inputLine.contains("idParaDest"))
                {
                    sb.append(inputLine.replaceAll("idParaDest-([0-9]+)\">", "idParaDest\\-$1\">" + String.valueOf
                            (cap++) + ". "))
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
                            inputLine.replaceAll("idParaDest-([0-9]+)\">", "idParaDest\\-$1\">$1. "))
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
            if (name.matches(".+-[0-9]+"))
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
                    sb.append("\t\t\t\t\t<p class=\"Epub-texto\">---Notas de rodapé---</p>").append("\n");
                    sb.append("\t\t\t\t</aside>\n");
                }
                else if (inputLine.contains("class=\"Epub-rodape\""))
                {
                    if (inputLine.contains("</a>. "))
                    {
                        sb.append(inputLine.replace("</a>", "</a>" + String.valueOf(++footNoteCount))).append("\n");
                    }
                    else
                    {
                        sb.append(inputLine.replace("</a>", "</a>" + String.valueOf(++footNoteCount) + ". ")).append
                                ("\n");
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
}
