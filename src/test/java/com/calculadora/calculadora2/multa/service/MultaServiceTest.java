package com.calculadora.calculadora2.multa.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class MultaServiceTest {
    private final MultaService service = new MultaService();
    @Test void usaDataDoCancelamentoEPercentualDidatico() {
        var r = service.calcular(LocalDate.of(2026,1,1), LocalDate.of(2026,10,15),12,new BigDecimal("30"));
        assertEquals(LocalDate.of(2027,1,1),r.fimContrato());
        assertEquals(78,r.diasRestantes());
        assertEquals(new BigDecimal("15.60"),r.valorMulta());
    }
    @Test void zeraDepoisDoPrazoEExcluiODiaJaFaturado() {
        var inicio=LocalDate.of(2026,1,1);
        assertEquals(new BigDecimal("0.00"),service.calcular(inicio,LocalDate.of(2027,1,1),12,new BigDecimal("30")).valorMulta());
        assertEquals(1,service.calcular(inicio,LocalDate.of(2026,12,31),12,new BigDecimal("30")).diasRestantes());
    }
    @Test void percentualPodeSerConfigurado() {
        var custom = new MultaService(new BigDecimal("0.10"));
        assertEquals(new BigDecimal("7.80"),custom.calcular(LocalDate.of(2026,1,1),LocalDate.of(2026,10,15),12,new BigDecimal("30")).valorMulta());
        assertThrows(IllegalArgumentException.class,()->new MultaService(new BigDecimal("1.01")));
    }
    @Test void rejeitaValoresNegativosEPrazoOuDatasInvalidas() {
        var d=LocalDate.of(2026,1,1);
        assertThrows(IllegalArgumentException.class,()->service.calcular(d,d,0,BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,()->service.calcular(d,d,12,new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class,()->service.calcular(d,d.minusDays(1),12,BigDecimal.ONE));
    }
}
