package br.senai.consultorio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Material realmente utilizado no atendimento.
 *
 * nome, unidade e preco sao COPIADOS do produto no momento do uso. Se o
 * material for renomeado ou reajustado depois, o comprovante ja emitido
 * continua contando a mesma historia.
 */
@Entity
@Table(name = "atendimento_material")
public class AtendimentoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false)
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "nome_produto", nullable = false, length = 120)
    private String nomeProduto;

    @Column(nullable = false, length = 20)
    private String unidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    /** true = material que NAO estava previsto no procedimento. */
    @Column(nullable = false)
    private boolean extra = false;

    protected AtendimentoMaterial() {
    }

    public AtendimentoMaterial(Produto produto, BigDecimal quantidade, boolean extra) {
        this.produto = produto;
        this.nomeProduto = produto.getNome();
        this.unidade = produto.getUnidade();
        this.precoUnitario = produto.getPrecoUnitario();
        this.quantidade = quantidade;
        this.extra = extra;
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
    }

    public void somarQuantidade(BigDecimal adicional) {
        this.quantidade = this.quantidade.add(adicional);
    }

    public Long getId() {
        return id;
    }

    public Atendimento getAtendimento() {
        return atendimento;
    }

    public void setAtendimento(Atendimento atendimento) {
        this.atendimento = atendimento;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public String getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public boolean isExtra() {
        return extra;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof AtendimentoMaterial material)) {
            return false;
        }
        return id != null && id.equals(material.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
