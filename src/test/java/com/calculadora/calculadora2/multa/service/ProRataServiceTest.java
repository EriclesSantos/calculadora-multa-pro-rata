package com.calculadora.calculadora2.multa.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class ProRataServiceTest {
    private final ProRataService service=new ProRataService();
    @Test void dividePelosDiasReaisDoMesCivil() {
        var r=service.calcular("ALFA",2,15,LocalDate.of(2026,10,15),new BigDecimal("31"));
        assertEquals(31,r.diasPeriodo());
        assertEquals(new BigDecimal("30.00"),r.valorTotal());
        assertEquals(29,service.calcular("BETA",1,1,LocalDate.of(2024,2,1),BigDecimal.ONE).diasPeriodo());
    }
    @Test void zeroDiasNaoGeraCobrancaEValoresInvalidosSaoRejeitados() {
        var d=LocalDate.of(2026,10,15);
        assertEquals(new BigDecimal("0.00"),service.calcular(2,0,d,BigDecimal.TEN).valorTotal());
        assertThrows(IllegalArgumentException.class,()->service.calcular(1,63,d,BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,()->service.calcular(1,1,d,new BigDecimal("-1")));
    }
}
