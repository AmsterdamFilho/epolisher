package br.org.neoteosofia.epolisher;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.lang.annotation.Annotation;

class ServidorCdi
{
    private static final ServidorCdi instance = new ServidorCdi();

    private final WeldContainer container;

    private ServidorCdi ()
    {
        Weld weld = new Weld();
        this.container = weld.initialize();
    }

    static ServidorCdi getServidor ()
    {
        return instance;
    }

    <T> T instanciar (Class<T> type, Annotation... qualifiers)
    {
        return container.select(type, qualifiers).get();
    }
}
