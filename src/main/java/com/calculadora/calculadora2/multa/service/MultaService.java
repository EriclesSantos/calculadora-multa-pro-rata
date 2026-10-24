package com.calculadora.calculadora2.multa.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class MultaService {

    private final BigDecimal percentualMulta;

    public MultaService() { this(new BigDecimal("0.20")); }

    @Autowired
    public MultaService(@Value("${calculadora.multa.percentual:0.20}") BigDecimal percentual) {
        if (percentual == null || percentual.signum() < 0 || percentual.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Percentual deve estar entre 0 e 1.");
        }
        this.percentualMulta = percentual;
    }

    public BigDecimal percentual() { return percentualMulta; }
    private static final BigDecimal DIAS_MES_COMERCIAL =
            new BigDecimal("30");
    private static final BigDecimal DIAS_ANO_COMERCIAL =
            new BigDecimal("365");
    private static final BigDecimal MESES_NO_ANO =
            new BigDecimal("12");

    public ResultadoMulta calcular(
            LocalDate dataInicio,
            LocalDate dataFimFaturamento,
            int mesesContrato,
            BigDecimal valorMensal
    ) {
        if (dataInicio == null || dataFimFaturamento == null || mesesContrato <= 0
                || valorMensal == null || valorMensal.signum() < 0) {
            throw new IllegalArgumentException("Informe datas, duracao e tarifa validas.");
        }
        if (dataFimFaturamento.isBefore(dataInicio)) {
            throw new IllegalArgumentException("Cancelamento anterior ao inicio do contrato.");
        }
        long diasContrato = DIAS_ANO_COMERCIAL
                .multiply(BigDecimal.valueOf(mesesContrato))
                .divide(MESES_NO_ANO, 0, RoundingMode.DOWN)
                .longValueExact();
        LocalDate fimContrato = dataInicio.plusDays(diasContrato);
        LocalDate inicioCalculo = dataFimFaturamento.plusDays(1);

        if (inicioCalculo.isAfter(fimContrato)) {
            return new ResultadoMulta(
                    fimContrato,
                    0,
                    BigDecimal.ZERO.setScale(2),
                    BigDecimal.ZERO
            );
        }

        long diasRestantes = ChronoUnit.DAYS.between(
                inicioCalculo,
                fimContrato
        ) + 1;

        BigDecimal valorMultaExato = valorMensal
                .multiply(BigDecimal.valueOf(diasRestantes))
                .divide(DIAS_MES_COMERCIAL, MathContext.DECIMAL128)
                .multiply(percentualMulta)
                .stripTrailingZeros();

        BigDecimal valorMulta = valorMultaExato.setScale(
                2,
                RoundingMode.HALF_UP
        );

        return new ResultadoMulta(
                fimContrato,
                diasRestantes,
                valorMulta,
                valorMultaExato
        );
    }

    public double calcularMulta(
            LocalDate dataInicio,
            LocalDate dataFimFaturamento,
            int mesesContrato,
            double valorMensal
    ) {
        return calcular(
                dataInicio,
                dataFimFaturamento,
                mesesContrato,
                BigDecimal.valueOf(valorMensal)
        ).valorMulta().doubleValue();
    }

    public record ResultadoMulta(
            LocalDate fimContrato,
            long diasRestantes,
            BigDecimal valorMulta,
            BigDecimal valorMultaExato
    ) {
    }
}
