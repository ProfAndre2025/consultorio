package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

/** Material odontologico. Todo dinheiro e BigDecimal -- nunca double. */
@Entity
@Table(name = "produto")
public class Produto {

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

    @Column(nullable = false, length = 120, unique = true)
    private String nome;

    @Column(nullable = false, length = 20)
    private String unidade = "un";

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    /** DECIMAL, e nao INT: existe material medido em gramas e mililitros. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal estoque = BigDecimal.ZERO;

    /*
     * Bloqueio otimista. Se dois atendimentos derem baixa no mesmo material ao
     * mesmo tempo, o segundo recebe OptimisticLockException em vez de gravar
     * um estoque errado por cima do outro.
     */
    @Version
    @Column(nullable = false)
    private Long versao;

    protected Produto() {
    }

    public Produto(String nome, String unidade, BigDecimal precoUnitario, BigDecimal estoque) {
        this.nome = nome;
        this.unidade = unidade;
        this.precoUnitario = precoUnitario;
        this.estoque = estoque;
    }

    /** Regra de negocio: o estoque nunca pode ficar negativo. */
    public void baixarEstoque(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraDeNegocioException("A quantidade utilizada deve ser maior que zero.");
        }
        if (estoque.compareTo(quantidade) < 0) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente de '%s': disponivel %s %s, necessario %s %s."
                            .formatted(nome, estoque.toPlainString(), unidade,
                                    quantidade.toPlainString(), unidade));
        }
        this.estoque = this.estoque.subtract(quantidade);
    }

    public void devolverAoEstoque(BigDecimal quantidade) {
        if (quantidade != null && quantidade.signum() > 0) {
            this.estoque = this.estoque.add(quantidade);
        }
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

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getEstoque() {
        return estoque;
    }

    public void setEstoque(BigDecimal estoque) {
        this.estoque = estoque;
    }

    public Long getVersao() {
        return versao;
    }

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

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Produto produto)) {
            return false;
        }
        return id != null && id.equals(produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
