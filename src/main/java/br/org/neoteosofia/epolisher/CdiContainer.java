package br.org.neoteosofia.epolisher;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.lang.annotation.Annotation;

class CdiContainer {

    private static final CdiContainer instance = new CdiContainer();

    private final WeldContainer container;

    private CdiContainer() {
        Weld weld = new Weld();
        this.container = weld.initialize();
    }

    static CdiContainer get() {
        return instance;
    }

    <T> T getInstance(Class<T> type, Annotation... qualifiers) {
        return container.select(type, qualifiers).get();
    }
}
