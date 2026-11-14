package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Formulario de agendamento.
 *
 * Data e hora chegam separadas porque na tela sao dois campos distintos
 * (input type=date e input type=time). O metodo dataHora() junta as duas.
 */
public class AgendamentoForm {

    @NotNull(message = "Escolha o paciente.")
    private Long pacienteId;

    @NotNull(message = "Escolha o procedimento.")
    private Long procedimentoId;

    /** Fica nulo enquanto o usuario nao clicou em "ver dentistas disponiveis". */
    private Long dentistaId;

    @NotNull(message = "Informe a data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o horario.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    @Size(max = 2000)
    private String observacoes;

    /** Junta data + hora. Anotado com @Future para a validacao pegar o passado. */
    @Future(message = "Nao e possivel agendar em data ou horario que ja passou.")
    public LocalDateTime getDataHora() {
        if (data == null || hora == null) {
            return null;
        }
        return LocalDateTime.of(data, hora);
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Long getProcedimentoId() {
        return procedimentoId;
    }

    public void setProcedimentoId(Long procedimentoId) {
        this.procedimentoId = procedimentoId;
    }

    public Long getDentistaId() {
        return dentistaId;
    }

    public void setDentistaId(Long dentistaId) {
        this.dentistaId = dentistaId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
