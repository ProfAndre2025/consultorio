package br.senai.consultorio.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuracao do banco de dados FEITA EM CODIGO, no lugar do application.yml.
 *
 * Segue o padrao do projeto "Sistema de Suporte Tecnico" do curso: sem Flyway,
 * com o proprio Hibernate criando e atualizando as tabelas a partir das
 * entidades (hbm2ddl.auto = update).
 *
 * A ordem de inicializacao e:
 *
 *   1. garantirBancoCriado()  -> conecta no SERVIDOR MySQL (sem escolher banco)
 *                                e executa CREATE DATABASE IF NOT EXISTS. E a
 *                                "verificacao do banco ja criado" que evita o
 *                                erro de "Unknown database 'consultorio'";
 *   2. dataSource()           -> o pool de conexoes ja aponta para o banco;
 *   3. entityManagerFactory() -> o Hibernate cria/atualiza as tabelas;
 *   4. carregarDadosExemplo() -> roda o seed idempotente (import.sql).
 *
 * Senha NUNCA fica escrita aqui: vem de variavel de ambiente.
 *   Linux/macOS:  export DB_PASSWORD=suasenha
 *   Windows:      $env:DB_PASSWORD="suasenha"
 */
@Configuration
@EnableTransactionManagement
public class DataConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataConfiguration.class);

    @Value("${DB_HOST:localhost}")
    private String host;

    @Value("${DB_PORT:3306}")
    private String porta;

    @Value("${DB_NAME:consultorio}")
    private String banco;

    @Value("${DB_USER:root}")
    private String usuario;

    @Value("${DB_PASSWORD:senha_do_banco}")
    private String senha;

    /**
     * Pool de conexoes com o MySQL, ja apontando para o banco da aplicacao.
     *
     * Antes de criar o pool, chamamos garantirBancoCriado(): sem isso, se o
     * banco ainda nao existisse, o proprio pool falharia ao subir com
     * "Unknown database". Essa e a verificacao que o enunciado pediu.
     */
    @Bean
    public DataSource dataSource() {
        garantirBancoCriado();

        String url = urlDoBanco();

        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url(url)
                .username(usuario)
                .password(senha)
                .build();
    }

    /**
     * EntityManagerFactory: a fabrica de "sessoes" do JPA/Hibernate.
     *
     * Com hbm2ddl.auto = update, o Hibernate compara as entidades com o banco e
     * cria/ajusta as tabelas que faltarem. E o padrao didatico do curso: simples
     * e sem ferramenta externa de migracao.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        HibernateJpaVendorAdapter adaptador = new HibernateJpaVendorAdapter();
        adaptador.setShowSql(false);

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(adaptador);
        emf.setPackagesToScan("br.senai.consultorio.domain.model");
        emf.setPersistenceUnitName("consultorioPU");
        emf.setJpaPropertyMap(propriedadesHibernate());
        return emf;
    }

    /**
     * Gerenciador de transacoes: e ele que faz o @Transactional funcionar.
     * Sem este bean, abrir e fechar transacao seria manual.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * Carrega os dados de exemplo DEPOIS que o contexto inteiro subiu -- ou seja,
     * depois que o Hibernate ja criou as tabelas. O ApplicationRunner e um gancho
     * do Spring Boot que roda uma unica vez, ao final da inicializacao.
     */
    @Bean
    public org.springframework.boot.ApplicationRunner carregadorDeDados(DataSource dataSource) {
        return argumentos -> carregarDadosExemplo(dataSource);
    }

    // ================================================================
    // Verificacao / criacao do banco
    // ================================================================

    /**
     * Conecta no SERVIDOR MySQL (URL sem nome de banco) e executa
     * CREATE DATABASE IF NOT EXISTS. Se o banco ja existe, o "IF NOT EXISTS"
     * faz o comando nao fazer nada -- por isso e seguro rodar sempre.
     */
    private void garantirBancoCriado() {
        String urlServidor = "jdbc:mysql://%s:%s/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo"
                .formatted(host, porta);

        String comando = "CREATE DATABASE IF NOT EXISTS `%s` "
                .formatted(banco)
                + "CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";

        try (Connection conexao = DriverManager.getConnection(urlServidor, usuario, senha);
             Statement stmt = conexao.createStatement()) {
            stmt.executeUpdate(comando);
            log.info("Banco de dados '{}' verificado/criado com sucesso.", banco);
        } catch (SQLException excecao) {
            throw new IllegalStateException(
                    "Nao foi possivel conectar ao servidor MySQL em %s:%s. "
                            .formatted(host, porta)
                            + "Verifique se o MySQL esta em execucao e se usuario/senha "
                            + "(variaveis DB_USER e DB_PASSWORD) estao corretos.", excecao);
        }
    }

    // ================================================================
    // Dados de exemplo (seed)
    // ================================================================

    /**
     * Le o arquivo import.sql do classpath e executa cada comando.
     *
     * O import.sql usa INSERT IGNORE, entao rodar de novo nao duplica nada nem
     * quebra por chave repetida -- o seed e idempotente.
     */
    private void carregarDadosExemplo(DataSource dataSource) {
        ClassPathResource recurso = new ClassPathResource("import.sql");
        if (!recurso.exists()) {
            return;
        }

        try (Connection conexao = dataSource.getConnection();
             Statement stmt = conexao.createStatement()) {

            for (String comando : lerComandos(recurso)) {
                stmt.execute(comando);
            }
            log.info("Dados de exemplo carregados (import.sql).");

        } catch (Exception excecao) {
            // O seed e opcional: se falhar, a aplicacao continua subindo.
            log.warn("Nao foi possivel carregar os dados de exemplo: {}", excecao.getMessage());
        }
    }

    /** Le o import.sql, ignora comentarios e separa os comandos pelo ';'. */
    private List<String> lerComandos(ClassPathResource recurso) throws Exception {
        StringBuilder conteudo = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String semEspaco = linha.strip();
                if (semEspaco.isEmpty() || semEspaco.startsWith("--")) {
                    continue;
                }
                conteudo.append(linha).append('\n');
            }
        }

        List<String> comandos = new ArrayList<>();
        for (String comando : conteudo.toString().split(";")) {
            if (!comando.isBlank()) {
                comandos.add(comando.strip());
            }
        }
        return comandos;
    }

    // ================================================================
    // Apoio
    // ================================================================

    private String urlDoBanco() {
        return "jdbc:mysql://%s:%s/%s".formatted(host, porta, banco)
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Sao_Paulo"
                + "&characterEncoding=UTF-8";
    }

    /** Ajustes do Hibernate que antes ficavam no bloco jpa.properties do YAML. */
    private Map<String, Object> propriedadesHibernate() {
        Map<String, Object> props = new HashMap<>();
        // "update": o Hibernate cria as tabelas que faltam e ajusta as existentes
        // a partir das entidades. E o padrao didatico do curso.
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.format_sql", true);
        props.put("hibernate.jdbc.time_zone", "America/Sao_Paulo");
        return props;
    }
}
