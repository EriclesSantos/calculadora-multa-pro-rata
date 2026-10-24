package com.calculadora.calculadora2.multa.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class EncerramentoController {

    private static final long ESPERA_RECARREGAMENTO_SEGUNDOS = 3;

    private final ConfigurableApplicationContext contexto;
    private final Set<String> sessoesAtivas = ConcurrentHashMap.newKeySet();
    private final Set<String> sessoesEncerradas = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService agendador =
            Executors.newSingleThreadScheduledExecutor(tarefa -> {
                Thread thread = new Thread(tarefa, "nordhub-encerramento");
                thread.setDaemon(true);
                return thread;
            });

    public EncerramentoController(ConfigurableApplicationContext contexto) {
        this.contexto = contexto;
    }

    @PostMapping("/aplicacao/atividade")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarAtividade(@RequestParam String sessao) {
        if (!sessao.isBlank() && !sessoesEncerradas.contains(sessao)) {
            sessoesAtivas.add(sessao);
        }
    }

    @PostMapping("/aplicacao/encerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrarSessao(@RequestParam String sessao) {
        if (sessao.isBlank()) {
            return;
        }

        sessoesEncerradas.add(sessao);
        sessoesAtivas.remove(sessao);

        agendador.schedule(
                () -> encerrarSeNaoHouverPaginaAberta(sessao),
                ESPERA_RECARREGAMENTO_SEGUNDOS,
                TimeUnit.SECONDS
        );
    }

    private void encerrarSeNaoHouverPaginaAberta(String sessaoEncerrada) {
        try {
            if (sessoesAtivas.isEmpty() && contexto.isActive()) {
                contexto.close();
            }
        } finally {
            sessoesEncerradas.remove(sessaoEncerrada);
        }
    }

    @PreDestroy
    void finalizarAgendador() {
        agendador.shutdown();
    }
}
