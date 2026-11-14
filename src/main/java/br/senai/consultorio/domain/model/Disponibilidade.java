package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/** Faixa de horario em que um dentista atende, em determinado dia da semana. */
@Entity
@Table(name = "disponibilidade")
public class Disponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    /** Gravado como texto (MONDAY, TUESDAY...). Nunca use ORDINAL em enum persistido. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    protected Disponibilidade() {
    }

    public Disponibilidade(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        if (horaInicio == null || horaFim == null || !horaInicio.isBefore(horaFim)) {
            throw new RegraDeNegocioException(
                    "O horario final deve ser maior que o horario inicial.");
        }
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }

    /** Duas faixas do mesmo dia nao podem se sobrepor. */
    public boolean sobrepoe(Disponibilidade outra) {
        return diaSemana == outra.diaSemana
                && horaInicio.isBefore(outra.horaFim)
                && horaFim.isAfter(outra.horaInicio);
    }

    public Long getId() {
        return id;
    }

    public Dentista getDentista() {
        return dentista;
    }

    public void setDentista(Dentista dentista) {
        this.dentista = dentista;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Disponibilidade disponibilidade)) {
            return false;
        }
        return id != null && id.equals(disponibilidade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
