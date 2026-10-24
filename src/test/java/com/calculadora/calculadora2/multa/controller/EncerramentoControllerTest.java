package com.calculadora.calculadora2.multa.controller;

import com.calculadora.calculadora2.multa.Calculadora2Application;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncerramentoControllerTest {

    @Test
    void recarregarMantemServidorEFecharUltimaPaginaEncerra() throws Exception {
        try (ConfigurableApplicationContext contexto = new SpringApplicationBuilder(
                Calculadora2Application.class
        ).run(
                "--server.port=0",
                "--calculadora.browser.auto-open=false"
        )) {
            int porta = ((ServletWebServerApplicationContext) contexto)
                    .getWebServer()
                    .getPort();
            HttpClient cliente = HttpClient.newHttpClient();

            postar(cliente, porta, "/aplicacao/atividade?sessao=pagina-antiga");
            postar(cliente, porta, "/aplicacao/encerrar?sessao=pagina-antiga");
            postar(cliente, porta, "/aplicacao/atividade?sessao=pagina-recarregada");

            Thread.sleep(3_500);
            assertTrue(contexto.isActive());

            postar(cliente, porta, "/aplicacao/encerrar?sessao=pagina-recarregada");

            long limite = System.nanoTime() + Duration.ofSeconds(6).toNanos();
            while (contexto.isActive() && System.nanoTime() < limite) {
                Thread.sleep(100);
            }

            assertFalse(contexto.isActive());
        }
    }

    private void postar(HttpClient cliente, int porta, String caminho) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + porta + caminho))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> resposta = cliente.send(
                requisicao,
                HttpResponse.BodyHandlers.discarding()
        );

        assertTrue(resposta.statusCode() >= 200 && resposta.statusCode() < 300);
    }
}
