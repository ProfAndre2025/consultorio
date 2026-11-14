package br.senai.consultorio.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AtendimentoView(
        Long id,
        Long consultaId,
        LocalDateTime realizadoEm,
        String pacienteNome,
        String pacienteCpf,
        String dentistaNome,
        String dentistaCro,
        String dentistaEspecialidade,
        String procedimentoNome,
        BigDecimal precoMaoObra,
        BigDecimal totalMateriais,
        BigDecimal valorTotal,
        boolean finalizado,
        String observacoes,
        List<MaterialUtilizadoView> materiais) {
}
