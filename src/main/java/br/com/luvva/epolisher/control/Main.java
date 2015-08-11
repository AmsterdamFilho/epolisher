package br.com.luvva.epolisher.control;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;

/**
 * @author Lima Filho, A. L. - amsterdam@luvva.com.br
 */
public class Main
{


    public static void main (String[] args)
    {
        File tocFile = getTocFile(args);
        if (tocFile != null)
        {
            try
            {
                addChapterNumbers(tocFile);
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(null, "Error editing the file: " + e.getMessage());
            }
        }
    }

    private static File getTocFile (String[] args)
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
                File oebps = epubDirectory.toPath().resolve("OEBPS").toFile();
                if (!(oebps.exists() && oebps.isDirectory()))
                {
                    JOptionPane.showMessageDialog(null, "OEBPS directory could not be resolved!");
                }
                else
                {
                    File tocFile = oebps.toPath().resolve("toc.xhtml").toFile();
                    if (!(tocFile.exists() && tocFile.isFile()))
                    {
                        JOptionPane.showMessageDialog(null, "toc.xhtml file could not be resolved!");
                    }
                    else
                    {
                        return tocFile;
                    }
                }
            }
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
}
