package br.senai.consultorio.dto.view;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Um item do historico clinico do paciente: um atendimento ja realizado,
 * na visao que interessa ao dentista no retorno.
 *
 * Deliberadamente SEM valores. O historico e clinico -- o que foi feito, por
 * quem, quando e com quais materiais --, nao financeiro. Precos e totais ficam
 * no comprovante e na tela do atendimento, nao aqui.
 *
 * 'materiais' e apenas a lista de nomes (com quantidade e unidade ja no texto),
 * porque o dentista quer saber O QUE foi usado, nao quanto custou.
 */
public record HistoricoItemView(
        Long atendimentoId,
        LocalDateTime realizadoEm,
        String procedimentoNome,
        String dentistaNome,
        String dentistaCro,
        List<String> materiais,
        String observacoes) {

    /** Verdadeiro quando ha observacoes clinicas para exibir. */
    public boolean temObservacoes() {
        return observacoes != null && !observacoes.isBlank();
    }

    /** Verdadeiro quando algum material foi registrado no atendimento. */
    public boolean temMateriais() {
        return materiais != null && !materiais.isEmpty();
    }
}
