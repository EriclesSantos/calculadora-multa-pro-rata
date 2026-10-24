package com.calculadora.calculadora2.multa.model;

import java.math.BigDecimal;

public record ResumoOperadora(
        String operadora,
        int chipsACancelar,
        int chipsComMulta,
        long diasMulta,
        BigDecimal tarifaMensal,
        BigDecimal multaCancelamento,
        BigDecimal reposicao,
        int diasProRata,
        int diasPeriodoProRata,
        BigDecimal valorDia,
        BigDecimal totalProRata
) {
}
