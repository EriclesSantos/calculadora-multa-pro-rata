package com.calculadora.calculadora2.multa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelaInicializacaoTest {

    @AfterEach
    void limparPropriedade() {
        System.clearProperty("calculadora.splash.enabled");
    }

    @Test
    void ficaHabilitadaPorPadrao() {
        assertTrue(TelaInicializacao.estaHabilitada(new String[0]));
    }

    @Test
    void podeSerDesabilitadaPorArgumento() {
        assertFalse(TelaInicializacao.estaHabilitada(
                new String[]{"--calculadora.splash.enabled=false"}
        ));
    }

    @Test
    void podeSerDesabilitadaPorPropriedadeDoJava() {
        System.setProperty("calculadora.splash.enabled", "false");

        assertFalse(TelaInicializacao.estaHabilitada(new String[0]));
    }
}
