package br.senai.consultorio.dto.view;

import java.math.BigDecimal;
import java.util.List;

public record ProcedimentoView(
        Long id,
        String nome,
        String descricao,
        BigDecimal precoMaoObra,
        Integer duracaoMinutos,
        BigDecimal precoSugerido,
        List<MaterialPadraoView> materiais) {
}
