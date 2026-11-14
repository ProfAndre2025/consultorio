package br.senai.consultorio.dto.view;

import java.math.BigDecimal;

/**
 * Versao leve do procedimento, usada em listagens e em <select>.
 *
 * Nao traz a lista de materiais de proposito: carregar os materiais de cada
 * linha da tabela dispararia uma consulta por procedimento (problema N+1).
 * Quem precisa do detalhe chama ProcedimentoService.buscarComMateriais().
 */
public record ProcedimentoResumoView(
        Long id,
        String nome,
        String descricao,
        BigDecimal precoMaoObra,
        Integer duracaoMinutos,
        boolean ativo) {

    public String rotulo() {
        return "%s (%d min)".formatted(nome, duracaoMinutos);
    }
}
