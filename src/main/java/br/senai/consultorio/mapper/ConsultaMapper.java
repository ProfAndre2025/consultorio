package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Atendimento;
import br.senai.consultorio.domain.model.Consulta;
import br.senai.consultorio.dto.view.ConsultaView;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConsultaMapper {

    /**
     * O atendimento e opcional porque a consulta pode ainda nao ter sido
     * atendida. Passar Optional deixa isso explicito na assinatura, em vez de
     * o chamador ter que adivinhar se pode mandar null.
     */
    public ConsultaView paraView(Consulta consulta, Optional<Atendimento> atendimento) {
        return new ConsultaView(
                consulta.getId(),
                consulta.getDataHora(),
                consulta.getDataHoraFim(),
                consulta.getPaciente().getNome(),
                consulta.getPaciente().getCpf(),
                consulta.getDentista().getId(),
                consulta.getDentista().getNome(),
                consulta.getDentista().getCro(),
                consulta.getProcedimento().getNome(),
                consulta.getProcedimento().getDuracaoMinutos(),
                consulta.getStatus(),
                consulta.getCanceladaPor(),
                consulta.getMotivoCancelamento(),
                consulta.getObservacoes(),
                atendimento.map(Atendimento::getId).orElse(null),
                atendimento.map(Atendimento::isFinalizado).orElse(false));
    }

    public ConsultaView paraView(Consulta consulta) {
        return paraView(consulta, Optional.empty());
    }
}
