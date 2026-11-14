package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class DisponibilidadeForm {

    @NotNull(message = "Escolha o dia da semana.")
    private DayOfWeek diaSemana;

    @NotNull(message = "Informe o horario inicial.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "Informe o horario final.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaFim;

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DayOfWeek diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }
}
