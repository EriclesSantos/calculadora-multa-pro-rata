package com.calculadora.calculadora2.multa.service;

public class FileDetector {

    public static String detectarFormato(String nomeArquivo) {

        nomeArquivo = nomeArquivo.toLowerCase();

        if (nomeArquivo.endsWith(".csv")) return "CSV";
        if (nomeArquivo.endsWith(".txt")) return "TXT";
        if (nomeArquivo.endsWith(".xlsx") || nomeArquivo.endsWith(".xls")) return "EXCEL";

        return "DESCONHECIDO";
    }
}
