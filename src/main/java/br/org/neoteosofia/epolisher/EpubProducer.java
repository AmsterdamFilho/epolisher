package br.org.neoteosofia.epolisher;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

@Singleton
public class EpubProducer
{
    private Epub epub;

    @PostConstruct
    private void init ()
    {
        String property = System.getProperty("epub");
        if (property == null || property.trim().isEmpty())
        {
            throw new RuntimeException("Please specify Epub directory for polishment!");
        }
        else
        {
            Path epubPath = Paths.get(property);
            if (!Files.exists(epubPath))
            {
                throw new RuntimeException("Specified Epub directory does not exist!");
            }
            else if (!Files.isDirectory(epubPath))
            {
                throw new RuntimeException("Choose Epub directory, not a file!");
            }
            else
            {
                Path oebps = epubPath.resolve("OEBPS");
                if (!(Files.exists(oebps) && Files.isDirectory(oebps)))
                {
                    throw new RuntimeException("OEBPS directory could not be resolved!");
                }
                else
                {
                    Path toc = oebps.resolve("toc.xhtml");
                    if (!(Files.exists(toc) && Files.isRegularFile(toc)))
                    {
                        throw new RuntimeException("toc.xhtml file could not be resolved!");
                    }
                    String title = getTitle(toc);
                    epub = new Epub()
                    {
                        @Override
                        public Path oebps ()
                        {
                            return oebps;
                        }

                        @Override
                        public Path toc ()
                        {
                            return toc;
                        }

                        @Override
                        public String title ()
                        {
                            return title;
                        }
                    };
                }
            }
        }
    }

    private String getTitle (Path toc)
    {
        try
        {
            String inputLine;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(Files.newInputStream(toc), UTF_8)))
            {
                while ((inputLine = in.readLine()) != null)
                {
                    if (inputLine.matches("\t\t<title>.+</title>"))
                    {
                        return
                                inputLine.
                                        replace("</title>", "").
                                        replace("<title>", "").
                                        replace("\t", "")
                                ;
                    }
                }
            }
            throw new RuntimeException("Could not find book's title!");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Produces
    private Epub produce ()
    {
        return epub;
    }
}
