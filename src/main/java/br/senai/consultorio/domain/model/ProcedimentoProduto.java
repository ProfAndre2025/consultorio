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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Vinculo entre um procedimento e um material que ele normalmente consome. */
@Entity
@Table(name = "procedimento_produto",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_procedimento_produto",
                columnNames = {"procedimento_id", "produto_id"}))
public class ProcedimentoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    protected ProcedimentoProduto() {
    }

    public ProcedimentoProduto(Produto produto, BigDecimal quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public BigDecimal calcularSubtotal() {
        return produto.getPrecoUnitario()
                .multiply(quantidade)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public Procedimento getProcedimento() {
        return procedimento;
    }

    public void setProcedimento(Procedimento procedimento) {
        this.procedimento = procedimento;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof ProcedimentoProduto vinculo)) {
            return false;
        }
        return id != null && id.equals(vinculo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
