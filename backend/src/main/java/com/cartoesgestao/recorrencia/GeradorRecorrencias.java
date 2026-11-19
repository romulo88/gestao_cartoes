package com.cartoesgestao.recorrencia;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gera as compras das recorrências na virada do dia e ao subir a aplicação (em dev o
 * backend não fica ligado à meia-noite; na VPS a subida cobre um restart perdido).
 */
@Component
public class GeradorRecorrencias {

    private static final Logger log = LoggerFactory.getLogger(GeradorRecorrencias.class);

    private final RecorrenciaRepository repository;
    private final RecorrenciaService service;

    public GeradorRecorrencias(RecorrenciaRepository repository, RecorrenciaService service) {
        this.repository = repository;
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoSubir() {
        gerar();
    }

    // Fuso da JVM = APP_TIMEZONE (America/Sao_Paulo), ver CartoesGestaoApplication.
    @Scheduled(cron = "0 5 0 * * *")
    public void diariamente() {
        gerar();
    }

    void gerar() {
        LocalDate hoje = LocalDate.now();
        for (Integer id : repository.idsPendentes(hoje)) {
            try {
                service.gerarPendentes(id, hoje);
            } catch (RuntimeException e) {
                // Uma recorrência com problema não pode travar as outras.
                log.error("Falha ao gerar a recorrência {}", id, e);
            }
        }
    }
}
