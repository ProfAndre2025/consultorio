package br.senai.consultorio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Ponto de extensao do Spring MVC.
 *
 * Por enquanto so registra os conversores de data e hora em pt-BR; deixamos a
 * classe pronta para futuras configuracoes (interceptors, recursos estaticos).
 */
@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // O Spring Boot ja registra os conversores de java.time automaticamente.
        // O metodo fica aqui como gancho didatico para novos formatadores.
    }
}
