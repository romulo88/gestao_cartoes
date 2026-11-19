package com.cartoesgestao;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sem o exclude, o Spring cria um usuário em memória e loga "Using generated security
// password" a cada subida - inútil aqui, o login é por JWT (ver security/).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling // geração diária das recorrências (recorrencia/GeradorRecorrencias)
public class CartoesGestaoApplication {

    private static final String FUSO_PADRAO = "America/Sao_Paulo";

    public static void main(String[] args) {
        // Mesma lição do Commander (v201): o container JRE alpine roda em UTC e o sistema
        // trabalha com LocalDate/LocalDateTime "de parede". Sem isso, uma compra lançada
        // depois das 21h ganharia a data do dia seguinte - e poderia cair na fatura errada.
        // Precisa rodar ANTES do Spring (o driver do Postgres lê o fuso da JVM).
        String fuso = System.getenv("APP_TIMEZONE");
        TimeZone.setDefault(TimeZone.getTimeZone(fuso == null || fuso.isBlank() ? FUSO_PADRAO : fuso));
        SpringApplication.run(CartoesGestaoApplication.class, args);
    }
}
