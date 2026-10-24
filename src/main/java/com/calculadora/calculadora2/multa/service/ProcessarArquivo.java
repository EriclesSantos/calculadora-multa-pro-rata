package com.calculadora.calculadora2.multa.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessarArquivo {

    public List<Object[]> processar(InputStream input) throws Exception {

        List<String> linhas = TextReader.lerLinhas(input);

        List<Object[]> linhasExcel = new ArrayList<>();

        for (String linha : linhas) {

            Map<String, String> dados =
                    LinhaParser.extrairDados(linha);

            linhasExcel.add(new Object[] {
                    dados.getOrDefault("iccid", ""),
                    dados.getOrDefault("linha", ""),
                    dados.getOrDefault("operadora", ""),
                    dados.getOrDefault("dataInicio", ""),
                    dados.getOrDefault("fimContrato", ""),
                    dados.getOrDefault("diasRestantes", ""),
                    dados.getOrDefault("multa", "")
            });
        }

        return linhasExcel;
    }
}