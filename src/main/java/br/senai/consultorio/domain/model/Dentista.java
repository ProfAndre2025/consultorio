package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "dentista")
public class Dentista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 20, unique = true)
    private String cro;

    @Column(length = 80)
    private String especialidade;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean ativo = true;

    /** Endereco embutido: as colunas ficam na propria tabela dentista. */
    @Embedded
    private Endereco endereco = new Endereco();

    /*
     * Hash BCrypt da senha que o dentista usa para iniciar um atendimento.
     * Guardamos SEMPRE o hash, nunca a senha em texto puro. Pode ser nulo:
     * dentista sem senha definida simplesmente ainda nao pode iniciar
     * atendimentos ate que a senha seja cadastrada.
     */
    @Column(name = "senha_atendimento", length = 100)
    private String senhaAtendimento;

    /*
     * A disponibilidade so existe dentro de um dentista: e o que chamamos de
     * "filho do agregado". Por isso cascade = ALL e orphanRemoval = true --
     * tirar da lista ja apaga do banco, sem precisar de repositorio proprio.
     */
    @OneToMany(mappedBy = "dentista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Disponibilidade> disponibilidades = new ArrayList<>();

    protected Dentista() {
    }

    public Dentista(String nome, String cro) {
        this.nome = nome;
        this.cro = cro;
    }

    /** Mantem os DOIS lados do relacionamento em sincronia e recusa sobreposicao. */
    public void adicionarDisponibilidade(Disponibilidade nova) {
        boolean conflita = disponibilidades.stream().anyMatch(nova::sobrepoe);
        if (conflita) {
            throw new RegraDeNegocioException(
                    "Ja existe uma faixa de horario cadastrada que se sobrepoe a esta.");
        }
        disponibilidades.add(nova);
        nova.setDentista(this);
    }

    public void removerDisponibilidade(Long disponibilidadeId) {
        Disponibilidade alvo = disponibilidades.stream()
                .filter(d -> Objects.equals(d.getId(), disponibilidadeId))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Faixa de horario nao pertence a este dentista."));
        disponibilidades.remove(alvo);
        alvo.setDentista(null);
    }

    public void inativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
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

    public String getCro() {
        return cro;
    }

    public void setCro(String cro) {
        this.cro = cro;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    // ---------------------------------------------------------------
    // Senha para iniciar atendimento
    // ---------------------------------------------------------------
    //
    // A criptografia (BCrypt) e injetada de fora: a entidade nao conhece o
    // Spring Security. Ela so decide as REGRAS -- se tem senha, e se a senha
    // digitada confere -- delegando o "como criptografar" ao PasswordEncoder.

    /** Define (ou troca) a senha, guardando apenas o hash. */
    public void definirSenha(String senhaEmTexto,
                             org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (senhaEmTexto == null || senhaEmTexto.isBlank()) {
            throw new RegraDeNegocioException("A senha nao pode ser vazia.");
        }
        if (senhaEmTexto.length() < 4) {
            throw new RegraDeNegocioException("A senha deve ter ao menos 4 caracteres.");
        }
        this.senhaAtendimento = encoder.encode(senhaEmTexto);
    }

    public boolean possuiSenha() {
        return senhaAtendimento != null && !senhaAtendimento.isBlank();
    }

    /** Confere a senha digitada contra o hash guardado. */
    public boolean senhaConfere(String senhaEmTexto,
                                org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (!possuiSenha() || senhaEmTexto == null) {
            return false;
        }
        return encoder.matches(senhaEmTexto, senhaAtendimento);
    }

    /** Usado pelo mapper apenas para saber se ha senha; nunca expoe o hash. */
    public String getSenhaAtendimento() {
        return senhaAtendimento;
    }

    public void setSenhaAtendimento(String senhaAtendimento) {
        this.senhaAtendimento = senhaAtendimento;
    }

    /** Lista imutavel: quem recebe nao consegue alterar o agregado por fora. */
    public List<Disponibilidade> getDisponibilidades() {
        return Collections.unmodifiableList(disponibilidades);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Dentista dentista)) {
            return false;
        }
        return id != null && id.equals(dentista.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
