package br.senai.consultorio.dto.view;

import java.util.List;

public record DentistaView(
        Long id,
        String nome,
        String cro,
        String especialidade,
        String telefone,
        String email,
        boolean ativo,
        List<DisponibilidadeView> disponibilidades) {
}
