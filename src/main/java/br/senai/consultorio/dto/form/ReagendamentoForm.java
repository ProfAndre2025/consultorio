package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Remarcacao de uma consulta ja agendada: nova data, novo horario e,
 * se for o caso, outro dentista.
 *
 * Paciente e procedimento NAO entram aqui de proposito. Trocar o paciente
 * seria outra consulta; trocar o procedimento mudaria a duracao, o preco e a
 * lista de materiais. Para esses casos o certo e cancelar e agendar de novo.
 */
public class ReagendamentoForm {

    @NotNull(message = "Escolha o dentista.")
    private Long dentistaId;

    @NotNull(message = "Informe a nova data.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Informe o novo horario.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;

    /** Junta data + hora. O @Future recusa remarcacao para o passado. */
    @Future(message = "Nao e possivel remarcar para data ou horario que ja passou.")
    public LocalDateTime getDataHora() {
        if (data == null || hora == null) {
            return null;
        }
        return LocalDateTime.of(data, hora);
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
}
