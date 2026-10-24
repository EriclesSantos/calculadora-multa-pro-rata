package com.calculadora.calculadora2.multa.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ProRataService {

    public ResultadoProRata calcular(
            String operadora,
            int quantidadeChips,
            int diasAFaturarPorChip,
            LocalDate dataFimFaturamento,
            BigDecimal tarifaMensal
    ) {
        if (quantidadeChips < 0 || diasAFaturarPorChip < 0 || diasAFaturarPorChip > 62
                || tarifaMensal == null || tarifaMensal.signum() < 0 || dataFimFaturamento == null) {
            throw new IllegalArgumentException("Quantidade e dias do pro rata nao podem ser negativos.");
        }

        int diasPeriodo = calcularDiasPeriodo(
                operadora,
                dataFimFaturamento
        );

        BigDecimal valorDia = tarifaMensal.divide(
                BigDecimal.valueOf(diasPeriodo),
                MathContext.DECIMAL128
        );

        BigDecimal totalExato = valorDia
                .multiply(BigDecimal.valueOf(quantidadeChips))
                .multiply(BigDecimal.valueOf(diasAFaturarPorChip))
                .stripTrailingZeros();

        BigDecimal total = totalExato.setScale(2, RoundingMode.HALF_UP);

        return new ResultadoProRata(
                valorDia,
                total,
                totalExato,
                diasPeriodo
        );
    }

    public ResultadoProRata calcular(
            int quantidadeChips,
            int diasAFaturarPorChip,
            LocalDate dataFimFaturamento,
            BigDecimal tarifaMensal
    ) {
        return calcular(
                "",
                quantidadeChips,
                diasAFaturarPorChip,
                dataFimFaturamento,
                tarifaMensal
        );
    }

    private int calcularDiasPeriodo(
            String operadora,
            LocalDate dataFimFaturamento
    ) {
        return dataFimFaturamento.lengthOfMonth();
    }

    public record ResultadoProRata(
            BigDecimal valorDia,
            BigDecimal valorTotal,
            BigDecimal valorTotalExato,
            int diasPeriodo
    ) {
    }
}
