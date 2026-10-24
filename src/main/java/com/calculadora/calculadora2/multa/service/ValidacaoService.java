package com.calculadora.calculadora2.multa.service;

import org.springframework.stereotype.Service;

@Service
public class ValidacaoService {

    public boolean validarIccid(String iccid) {

        if (iccid == null) {
            return false;
        }

        iccid = iccid.trim();

        if (iccid.isEmpty()) {
            return false;
        }

        if (!iccid.matches("\\d+")) {
            return false;
        }

        return iccid.length() >= 19
                && iccid.length() <= 22;
    }

    public boolean validarLinha(String linha) {

        if (linha == null) {
            return false;
        }

        linha = linha.trim();

        if (linha.isEmpty()) {
            return false;
        }

        if (!linha.matches("\\d+")) {
            return false;
        }

        return linha.length() >= 12
                && linha.length() <= 14;
    }

    public boolean validarOperadora(String operadora) {

        if (operadora == null) {
            return false;
        }

        return !operadora.trim().isEmpty();
    }

    public boolean validarData(String data) {

        if (data == null) {
            return false;
        }

        data = data.trim();

        if (data.isEmpty()) {
            return false;
        }

        String apenasData = data.split("[ T]", 2)[0];

        return apenasData.matches("\\d{1,2}/\\d{1,2}/\\d{4}")
                || apenasData.matches("\\d{4}-\\d{1,2}-\\d{1,2}")
                || apenasData.matches("\\d{1,2}-\\d{1,2}-\\d{4}");
    }

    public boolean validarTarifa(Double tarifa) {
        return tarifa != null && tarifa > 0;
    }
}
