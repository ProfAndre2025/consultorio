package br.senai.consultorio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * LGPD: registros nao sao apagados. Ao "excluir", o cadastro fica
     * inativo (ativo = false) -- some das listagens, mas continua no banco
     * para consulta e pode ser reativado.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ativo = true;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 14, unique = true)
    private String cpf;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(name = "estado_civil", length = 20)
    private String estadoCivil;

    @Column(length = 20)
    private String genero;

    /** Endereco embutido: as colunas ficam na propria tabela paciente. */
    @Embedded
    private Endereco endereco = new Endereco();

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /** LGPD: "exclusao" na verdade inativa o registro. */
    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    /** Construtor sem argumentos exigido pelo JPA. Nao use no seu codigo. */
    protected Paciente() {
    }

    public Paciente(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    /*
     * equals/hashCode baseados SOMENTE no id.
     * Comparar todos os campos quebra assim que a entidade entra em um Set
     * antes de ser salva (id ainda nulo) ou quando o Hibernate devolve um proxy.
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Paciente paciente)) {
            return false;
        }
        return id != null && id.equals(paciente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
