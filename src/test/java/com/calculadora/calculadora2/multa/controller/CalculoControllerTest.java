package com.calculadora.calculadora2.multa.controller;

import com.calculadora.calculadora2.multa.service.MultaService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculoControllerTest {

    private final CalculoController controller = new CalculoController(new MultaService());

    @Test
    void usaADataInformadaEmVezDaDataAtual() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.calcular(
                "CLIENTE DEMONSTRACAO",
                "2026-07-16",
                null,
                "89000000000000000006",
                "5500000000002",
                "ALFA",
                "2025-10-03",
                12,
                6.0,
                null,
                model
        );

        assertEquals("3.16", model.get("multa"));
    }

    @Test
    void usaOFimDoFaturamentoComoDataBaseQuandoFoiInformado() {
        ExtendedModelMap model = new ExtendedModelMap();

        controller.calcular(
                "CLIENTE DEMONSTRACAO",
                "2026-07-16",
                "2026-08-15",
                "89000000000000000006",
                "5500000000002",
                "ALFA",
                "2025-10-03",
                12,
                6.0,
                null,
                model
        );

        assertEquals("1.96", model.get("multa"));
    }
}
