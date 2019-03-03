package br.org.neoteosofia.epolisher;

import javax.inject.Inject;
import javax.swing.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Lima Filho, A. L. - amsterdamluis.40@gmail.com
 */
public class Epolisher
{
    private @Inject IndexPolisher       indexPolisher;
    private @Inject PagesTitlesPolisher pagesTitlesPolisher;
    private @Inject FootnotePolisher    footnotePolisher;

    public static void main (String[] args)
    {
        try
        {
            ServidorCdi.getServidor().instanciar(Epolisher.class).start();
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            Path log = Paths.get(System.getProperty("user.home")).resolve("Desktop").resolve("epolisherLog.log");
            try
            {
                ex.printStackTrace(new PrintStream(Files.newOutputStream(log)));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void start () throws IOException
    {
        indexPolisher.start();
        pagesTitlesPolisher.start();
        footnotePolisher.start();
    }
}
