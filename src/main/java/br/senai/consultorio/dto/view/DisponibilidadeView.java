package br.senai.consultorio.dto.view;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadeView(
        Long id,
        DayOfWeek diaSemana,
        String diaSemanaDescricao,
        LocalTime horaInicio,
        LocalTime horaFim) {
}
