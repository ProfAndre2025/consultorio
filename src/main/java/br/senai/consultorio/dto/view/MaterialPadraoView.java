package br.senai.consultorio.dto.view;

import java.math.BigDecimal;

/** Material da lista padrao de um procedimento. */
public record MaterialPadraoView(
        Long id,
        Long produtoId,
        String nome,
        String unidade,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal) {
}
