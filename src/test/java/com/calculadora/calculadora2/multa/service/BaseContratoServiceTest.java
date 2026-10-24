package com.calculadora.calculadora2.multa.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class BaseContratoServiceTest {
    @Test void carregaSomenteTresContratosSinteticos() {
        BaseContratoService service = new BaseContratoService();
        assertEquals(3, service.quantidadeRegistros());
        assertEquals(LocalDate.of(2026,1,1), service.buscarDataInicio("00000000000000000001").orElseThrow());
        assertTrue(service.buscarDataInicio("89999999999999999999").isEmpty());
    }
}
