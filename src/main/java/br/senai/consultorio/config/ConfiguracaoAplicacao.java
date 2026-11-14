package br.senai.consultorio.config;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracoes da aplicacao que NAO sao do banco -- o que antes ficava fora
 * do bloco "spring.datasource/jpa/flyway" do application.yml.
 *
 * Aqui ficam a porta do servidor e os dados da clinica usados no comprovante.
 * A configuracao do banco em si esta em DataConfiguration.
 */
@Configuration
public class ConfiguracaoAplicacao {

    /**
     * Dados da clinica impressos no comprovante em PDF.
     *
     * Ficam em codigo por simplicidade didatica. Num sistema real, valores como
     * estes normalmente viriam de uma tela de "configuracoes da clinica" gravada
     * no proprio banco.
     */
    @Bean
    public PropriedadesClinica propriedadesClinica() {
        return new PropriedadesClinica(
                new PropriedadesClinica.Clinica(
                        "Consultorio Odontologico Sorriso",
                        "Rua das Acacias, 120 - Jardim Primavera - Sao Paulo/SP - CEP 01234-000",
                        "(11) 3000-0000",
                        "00.000.000/0001-00",
                        // Coloque o arquivo do logo neste caminho (PNG ou JPG).
                        // Se nao existir, os PDFs saem sem logo, sem erro.
                        "./documentos/logo.png"),
                new PropriedadesClinica.Documentos("./documentos"));
    }

    /** Porta em que o Tomcat embutido sobe (antes era server.port no YAML). */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> portaDoServidor() {
        return fabrica -> fabrica.setPort(8080);
    }

    /**
     * Algoritmo de hash das senhas dos dentistas (BCrypt).
     *
     * BCrypt embute um "sal" aleatorio em cada hash e e proposital-mente lento,
     * o que dificulta ataques de forca bruta. Guardar a senha assim -- e nunca
     * em texto puro -- e o basico de seguranca mesmo num projeto didatico.
     */
    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
