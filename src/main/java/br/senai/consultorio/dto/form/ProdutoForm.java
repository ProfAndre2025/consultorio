package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProdutoForm {

    @NotBlank(message = "Informe o nome do material.")
    @Size(min = 2, max = 120)
    private String nome;

    @NotBlank(message = "Informe a unidade de medida.")
    @Size(max = 20)
    private String unidade = "un";

    @NotNull(message = "Informe o preco unitario.")
    @DecimalMin(value = "0.00", message = "O preco nao pode ser negativo.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    @NotNull(message = "Informe o estoque.")
    @DecimalMin(value = "0.00", message = "O estoque nao pode ser negativo.")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal estoque = BigDecimal.ZERO;

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
}
