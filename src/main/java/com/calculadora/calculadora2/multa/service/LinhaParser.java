package com.calculadora.calculadora2.multa.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinhaParser {

    private static final Pattern ICCID_PATTERN = Pattern.compile("(?<!\\d)(\\d{19,22})(?!\\d)");
    private static final Pattern LINHA_PATTERN = Pattern.compile("(?<!\\d)(\\d{12,14})(?!\\d)");
    private static final Pattern DATA_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,2}/\\d{1,2}/\\d{4}|\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}-\\d{1,2}-\\d{4})(?!\\d)"
    );
    private static final Set<String> OPERADORAS_PADRAO = Set.of(
            "VIVO", "TIM", "CLARO", "OI", "VODAFONE",
            "CONECTA", "EMNIFY", "ARQIA", "LINKSFIELD"
    );

    private LinhaParser() {
    }

    public static Map<String, String> extrairDados(String linha) {
        return extrairDados(linha, OPERADORAS_PADRAO);
    }

    public static Map<String, String> extrairDados(
            String linha,
            Set<String> operadorasCadastradas
    ) {
        Map<String, String> dados = new HashMap<>();

        if (linha == null || linha.isBlank()) {
            return dados;
        }

        String iccid = encontrarNumeroPreferido(ICCID_PATTERN, linha, "89");
        String numeroLinha = encontrarNumeroPreferido(LINHA_PATTERN, linha, "55");
        String data = encontrarPrimeiro(DATA_PATTERN, linha);
        String operadora = encontrarOperadora(linha, operadorasCadastradas);

        adicionarSePreenchido(dados, "iccid", iccid);
        adicionarSePreenchido(dados, "linha", numeroLinha);
        adicionarSePreenchido(dados, "operadora", operadora);
        adicionarSePreenchido(dados, "dataInicio", data);

        return dados;
    }

    public static String normalizarOperadora(String operadora) {
        if (operadora == null) {
            return "";
        }

        return removerAcentos(operadora)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");
    }

    private static String encontrarOperadora(
            String linha,
            Set<String> operadorasCadastradas
    ) {
        String texto = removerAcentos(linha)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
        String textoDelimitado = " " + texto + " ";

        return operadorasCadastradas.stream()
                .map(LinhaParser::normalizarOperadora)
                .filter(operadora -> !operadora.isBlank())
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .filter(operadora -> textoDelimitado.contains(" " + operadora + " "))
                .findFirst()
                .orElse("");
    }

    private static String encontrarNumeroPreferido(
            Pattern pattern,
            String linha,
            String prefixoPreferido
    ) {
        Matcher matcher = pattern.matcher(linha);
        List<String> encontrados = new ArrayList<>();

        while (matcher.find()) {
            encontrados.add(matcher.group(1));
        }

        return encontrados.stream()
                .filter(valor -> valor.startsWith(prefixoPreferido))
                .findFirst()
                .orElse(encontrados.isEmpty() ? "" : encontrados.get(0));
    }

    private static String encontrarPrimeiro(Pattern pattern, String linha) {
        Matcher matcher = pattern.matcher(linha);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static void adicionarSePreenchido(
            Map<String, String> dados,
            String chave,
            String valor
    ) {
        if (valor != null && !valor.isBlank()) {
            dados.put(chave, valor);
        }
    }

    private static String removerAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}

