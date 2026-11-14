package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Usado para vincular um material a um procedimento. */
public class VincularMaterialForm {

    @NotNull(message = "Escolha o material.")
    private Long produtoId;

    @NotNull(message = "Informe a quantidade.")
    @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal quantidade = BigDecimal.ONE;

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
