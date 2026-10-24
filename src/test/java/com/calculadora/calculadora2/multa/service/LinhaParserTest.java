package com.calculadora.calculadora2.multa.service;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinhaParserTest {

    @Test
    void extraiSomenteOsCamposNecessariosDeUmaLinhaSuja() {
        String linha = "PROTOCOLO 12345678901 | ICCID=89000000000000000006; "
                + "codigo 7654321; telefone: 5500000000002; VIVO ### 03/10/2025; valor 999";

        Map<String, String> dados = LinhaParser.extrairDados(linha, Set.of("VIVO"));

        assertEquals("89000000000000000006", dados.get("iccid"));
        assertEquals("5500000000002", dados.get("linha"));
        assertEquals("VIVO", dados.get("operadora"));
        assertEquals("03/10/2025", dados.get("dataInicio"));
        assertEquals(4, dados.size());
    }

    @Test
    void deixaUmaLinhaDeSujeiraSemDadosSuficientesParaProcessamento() {
        Map<String, String> dados = LinhaParser.extrairDados(
                "TOTAL 8900000000000000000009 observacao sem cadastro",
                Set.of("VIVO")
        );

        assertTrue(dados.size() < 2);
    }

    @Test
    void ignoraAHoraDeUmaDataNoPadraoDaPlanilhaExemplo() {
        String linha = "CONTRATO-DEMO 00 89000000000000000005 Ativo "
                + "5500000000004 20MB U Desconhecido 31/03/2026 18:23:19 "
                + "CLARO CONTRATO-DEMO 00";

        Map<String, String> dados = LinhaParser.extrairDados(linha, Set.of("CLARO"));

        assertEquals("89000000000000000005", dados.get("iccid"));
        assertEquals("5500000000004", dados.get("linha"));
        assertEquals("CLARO", dados.get("operadora"));
        assertEquals("31/03/2026", dados.get("dataInicio"));
        assertEquals(4, dados.size());
    }

    @Test
    void extraiDataAmericanaComHoraMinutoSegundoEPeriodo() {
        String linha = "sujeira 89000000000000000005 status 5500000000004 "
                + "03/31/2026 06:23:19 PM CLARO outros 999";

        Map<String, String> dados = LinhaParser.extrairDados(linha, Set.of("CLARO"));

        assertEquals("03/31/2026", dados.get("dataInicio"));
        assertEquals("89000000000000000005", dados.get("iccid"));
        assertEquals("5500000000004", dados.get("linha"));
        assertEquals("CLARO", dados.get("operadora"));
    }
}
