package com.calculadora.calculadora2.multa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class Calculadora2Application {

    static final String PAGINA_INICIAL = "http://127.0.0.1:48271/";

    public static void main(String[] args) {
        TelaInicializacao.exibir(args);
        TelaInicializacao.atualizar("Verificando o servidor local...");

        if (aplicacaoJaEstaExecutando()) {
            TelaInicializacao.atualizar(
                    "A calculadora já está ativa. Abrindo o navegador..."
            );
            if (BrowserLauncher.abrir(PAGINA_INICIAL)) {
                TelaInicializacao.concluir();
            } else {
                TelaInicializacao.informarFalha(
                        "A calculadora já está ativa, mas o navegador não pôde ser aberto.\n"
                                + "Acesse manualmente: " + PAGINA_INICIAL
                );
            }
            return;
        }

        TelaInicializacao.atualizar("Iniciando o servidor local...");
        try {
            SpringApplication.run(
                    Calculadora2Application.class,
                    args
            );
        } catch (Exception erro) {
            TelaInicializacao.informarFalha(
                    "O servidor local não pôde ser iniciado.\n"
                            + "Verifique se outra versão da calculadora está aberta."
            );
            throw erro;
        }
    }

    private static boolean aplicacaoJaEstaExecutando() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create(PAGINA_INICIAL)
                    .toURL()
                    .openConnection();

            connection.setConnectTimeout(600);
            connection.setReadTimeout(1_000);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            try (InputStream input = connection.getInputStream()) {
                String pagina = new String(
                        input.readNBytes(16_384),
                        StandardCharsets.UTF_8
                );
                return pagina.contains(
                        "<meta name=\"application-name\" content=\"nordhub-calculadora\">"
                );
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
