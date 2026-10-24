package com.calculadora.calculadora2.multa.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TextReader {

    public static List<String> lerLinhas(InputStream input) throws IOException {

        List<String> linhas = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        String linha;

        while((linha = reader.readLine()) != null){

            if(!linha.trim().isEmpty()){
                linhas.add(linha);
            }
        }

        return linhas;
    }
}
