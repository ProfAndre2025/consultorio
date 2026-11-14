package br.senai.consultorio.dto.view;

import java.math.BigDecimal;

public record MaterialUtilizadoView(
        Long id,
        Long produtoId,
        String nome,
        String unidade,
        BigDecimal quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal,
        boolean extra) {

    public String origem() {
        return extra ? "Adicional" : "Previsto";
    }
}
