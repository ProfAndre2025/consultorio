package br.senai.consultorio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Ponto de entrada da aplicacao.
 *
 * Ao subir, a aplicacao:
 *   1. verifica/cria o banco (CREATE DATABASE IF NOT EXISTS);
 *   2. deixa o Hibernate criar/atualizar as tabelas a partir das entidades;
 *   3. carrega os dados de exemplo (import.sql);
 *   4. levanta o Tomcat embutido na porta 8080.
 *
 * Note os "exclude" abaixo: normalmente o Spring Boot monta o DataSource e o
 * JPA automaticamente. Neste projeto quem faz isso e a classe DataConfiguration,
 * escrita a mao. Por isso desligamos as duas auto-configuracoes
 * correspondentes -- assim nao existem duas fontes de verdade brigando pela
 * configuracao do banco.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "br.senai.consultorio.repository")
public class ConsultorioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultorioApplication.class, args);
    }
}
