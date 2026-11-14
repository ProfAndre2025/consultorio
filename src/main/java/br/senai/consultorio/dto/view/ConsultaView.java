package br.senai.consultorio.dto.view;

import br.senai.consultorio.domain.enums.OrigemCancelamento;
import br.senai.consultorio.domain.enums.StatusConsulta;

import java.time.LocalDateTime;

public record ConsultaView(
        Long id,
        LocalDateTime dataHora,
        LocalDateTime dataHoraFim,
        String pacienteNome,
        String pacienteCpf,
        Long dentistaId,
        String dentistaNome,
        String dentistaCro,
        String procedimentoNome,
        Integer duracaoMinutos,
        StatusConsulta status,
        OrigemCancelamento canceladaPor,
        String motivoCancelamento,
        String observacoes,
        Long atendimentoId,
        boolean atendimentoFinalizado) {

    /** Ex.: "Cancelada (dentista)". Evita colocar logica de texto no template. */
    public String situacao() {
        if (status == StatusConsulta.CANCELADA && canceladaPor != null) {
            return "%s (%s)".formatted(status.getDescricao(),
                    canceladaPor.getDescricao().toLowerCase());
        }
        return status.getDescricao();
    }

    public boolean podeIniciarAtendimento() {
        return status != StatusConsulta.CANCELADA && !atendimentoFinalizado;
    }
}
