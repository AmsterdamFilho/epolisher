package br.org.neoteosofia.epolisher;

import java.nio.file.Path;

public interface Epub {

    String CD_I = "A Criação de Deus (Volume I)";
    String CD_III = "A Criação de Deus (Volume III)";
    String D = "O Decálogo";
    String EXP = "Explicações de Textos das Escrituras Sagradas";
    String GEJ_I = "O Grande Evangelho de João (Volume I)";
    String GEJ_X = "O Grande Evangelho de João (Volumes X e XI)";
    String IJ = "A Infância de Jesus";
    String MP = "Mensagens do Pai";
    String PS = "Prédicas do Senhor";
    String RB_II = "Roberto Blum (Volume II)";
    String TL = "A Terra e a Lua";

    Path oebps();

    Path toc();

    String title();
}
