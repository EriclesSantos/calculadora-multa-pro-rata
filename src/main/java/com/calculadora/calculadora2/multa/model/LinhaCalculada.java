package com.calculadora.calculadora2.multa.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LinhaCalculada(
        String iccid,
        String linha,
        String operadora,
        LocalDate inicioContrato,
        LocalDate fimContrato,
        long diasRestantes,
        BigDecimal multa
) {
}
