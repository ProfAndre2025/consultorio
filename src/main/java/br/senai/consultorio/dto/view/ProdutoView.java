package br.senai.consultorio.dto.view;

import java.math.BigDecimal;

public record ProdutoView(
        Long id,
        String nome,
        String unidade,
        BigDecimal precoUnitario,
        BigDecimal estoque,
        boolean ativo) {

    /** Rotulo pronto para os <select> de material. */
    public String rotulo() {
        return "%s (%s / %s)".formatted(nome, precoUnitario.toPlainString(), unidade);
    }
}
