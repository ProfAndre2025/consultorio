package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProcedimentoForm {

    @NotBlank(message = "Informe o nome do procedimento.")
    @Size(min = 3, max = 120)
    private String nome;

    @Size(max = 2000)
    private String descricao;

    @NotNull(message = "Informe o valor dos honorarios do dentista.")
    @DecimalMin(value = "0.00", message = "O valor nao pode ser negativo.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal precoMaoObra = BigDecimal.ZERO;

    @NotNull(message = "Informe a duracao.")
    @Min(value = 5, message = "A duracao minima e de 5 minutos.")
    @Max(value = 480, message = "A duracao maxima e de 480 minutos.")
    private Integer duracaoMinutos = 30;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoMaoObra() {
        return precoMaoObra;
    }

    public void setPrecoMaoObra(BigDecimal precoMaoObra) {
        this.precoMaoObra = precoMaoObra;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }
}
