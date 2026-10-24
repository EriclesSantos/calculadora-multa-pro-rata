package com.calculadora.calculadora2.multa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

@Component
public class BrowserLauncher {

    private final boolean abrirAutomaticamente;

    public BrowserLauncher(
            @Value("${calculadora.browser.auto-open:true}")
            boolean abrirAutomaticamente
    ) {
        this.abrirAutomaticamente = abrirAutomaticamente;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void abrirQuandoEstiverPronto() {
        if (abrirAutomaticamente) {
            TelaInicializacao.atualizar(
                    "Servidor iniciado. Abrindo o navegador..."
            );
            if (!abrir(Calculadora2Application.PAGINA_INICIAL)) {
                TelaInicializacao.informarFalha(
                        "O servidor foi iniciado, mas o navegador não pôde ser aberto.\n"
                                + "Acesse manualmente: "
                                + Calculadora2Application.PAGINA_INICIAL
                );
                return;
            }
        }

        TelaInicializacao.concluir();
    }

    static boolean abrir(String endereco) {
        try {
            String sistema = System.getProperty("os.name", "")
                    .toLowerCase(Locale.ROOT);

            if (sistema.contains("win")) {
                new ProcessBuilder(
                        "rundll32",
                        "url.dll,FileProtocolHandler",
                        endereco
                ).start();
                return true;
            }

            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(endereco));
                return true;
            }

            if (sistema.contains("mac")) {
                new ProcessBuilder("open", endereco).start();
            } else {
                new ProcessBuilder("xdg-open", endereco).start();
            }
            return true;
        } catch (Exception erro) {
            System.err.println(
                    "Nao foi possivel abrir o navegador automaticamente: "
                            + erro.getMessage()
            );
            return false;
        }
    }
}
