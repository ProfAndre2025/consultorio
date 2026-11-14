package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Atendimento;
import br.senai.consultorio.domain.model.AtendimentoMaterial;
import br.senai.consultorio.domain.model.Consulta;
import br.senai.consultorio.dto.view.AtendimentoView;
import br.senai.consultorio.dto.view.MaterialUtilizadoView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AtendimentoMapper {

    public AtendimentoView paraView(Atendimento atendimento) {
        Consulta consulta = atendimento.getConsulta();

        List<MaterialUtilizadoView> materiais = atendimento.getMateriais().stream()
                .sorted(Comparator.comparing((AtendimentoMaterial m) -> m.isExtra())
                        .thenComparing(AtendimentoMaterial::getNomeProduto))
                .map(this::paraView)
                .toList();

        return new AtendimentoView(
                atendimento.getId(),
                consulta.getId(),
                atendimento.getRealizadoEm(),
                consulta.getPaciente().getNome(),
                consulta.getPaciente().getCpf(),
                consulta.getDentista().getNome(),
                consulta.getDentista().getCro(),
                consulta.getDentista().getEspecialidade(),
                consulta.getProcedimento().getNome(),
                atendimento.getPrecoMaoObra(),
                atendimento.calcularTotalMateriais(),
                atendimento.getValorTotal(),
                atendimento.isFinalizado(),
                atendimento.getObservacoes(),
                materiais);
    }

    public MaterialUtilizadoView paraView(AtendimentoMaterial material) {
        return new MaterialUtilizadoView(
                material.getId(),
                material.getProduto().getId(),
                material.getNomeProduto(),
                material.getUnidade(),
                material.getQuantidade(),
                material.getPrecoUnitario(),
                material.calcularSubtotal(),
                material.isExtra());
    }
}
