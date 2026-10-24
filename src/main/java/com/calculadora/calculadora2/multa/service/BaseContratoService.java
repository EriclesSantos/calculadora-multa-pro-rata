package com.calculadora.calculadora2.multa.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BaseContratoService {

    private static final String ARQUIVO_BASE = "base-contratos.tsv";
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);

    private final Map<String, LocalDate> datasPorIccid;

    public BaseContratoService() {
        this.datasPorIccid = carregarBase();
    }

    public Optional<LocalDate> buscarDataInicio(String iccid) {
        return Optional.ofNullable(datasPorIccid.get(iccid));
    }

    public int quantidadeRegistros() {
        return datasPorIccid.size();
    }

    private Map<String, LocalDate> carregarBase() {
        Map<String, LocalDate> datas = new LinkedHashMap<>();
        ClassPathResource resource = new ClassPathResource(ARQUIVO_BASE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String linha;
            int numeroLinha = 0;
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                linha = linha.replace("\uFEFF", "").trim();
                if (linha.isEmpty()) {
                    continue;
                }

                String[] campos = linha.split("\\s+");
                if (campos.length < 4 || !campos[0].matches("\\d{19,22}")) {
                    throw new IllegalStateException(
                            "Registro invalido na linha " + numeroLinha + " da base de contratos."
                    );
                }

                String iccid = campos[0];
                LocalDate dataInicio = LocalDate.parse(campos[3], FORMATO_DATA);
                LocalDate anterior = datas.putIfAbsent(iccid, dataInicio);
                if (anterior != null && !anterior.equals(dataInicio)) {
                    throw new IllegalStateException(
                            "ICCID duplicado com datas diferentes na base de contratos."
                    );
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Nao foi possivel carregar a base historica de contratos.", e);
        }

        if (datas.isEmpty()) {
            throw new IllegalStateException("A base historica de contratos esta vazia.");
        }

        return Collections.unmodifiableMap(datas);
    }
}
