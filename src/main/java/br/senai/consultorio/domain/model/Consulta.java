package br.senai.consultorio.domain.model;

import br.senai.consultorio.domain.enums.OrigemCancelamento;
import br.senai.consultorio.domain.enums.StatusConsulta;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agendamento de uma consulta.
 *
 * As regras de transicao de estado (agendada -> realizada / cancelada) moram
 * AQUI, dentro da propria entidade. O service apenas orquestra; ele nao decide
 * se um cancelamento e valido. Isso evita que a mesma regra seja reescrita em
 * cada tela ou endpoint que mexer em consultas.
 */
@Entity
@Table(
    name = "consulta",
    /*
     * Ultima linha de defesa contra agendamento duplicado.
     *
     * A validacao no AgendaService ja recusa o conflito, mas ela roda ANTES do
     * commit: dois atendentes (ou dois cliques rapidos) podem passar pela
     * checagem ao mesmo tempo e gravar duas consultas iguais. So o banco,
     * com uma restricao fisica, fecha essa janela de corrida.
     *
     * A regra e "um dentista nao pode ter DUAS consultas AGENDADAS comecando no
     * mesmo instante". Consultas canceladas ou realizadas nao contam -- por
     * isso o indice nao e sobre (dentista_id, data_hora) direto, e sim sobre a
     * coluna gerada 'chave_agenda' abaixo, que so tem valor quando a consulta
     * esta AGENDADA e fica NULL nos demais casos. No MySQL varios NULL nao
     * colidem num indice unico, entao remarcar apos cancelar continua livre.
     */
    uniqueConstraints = @UniqueConstraint(
        name = "uk_consulta_dentista_horario",
        columnNames = {"dentista_id", "chave_agenda"}))
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    /*
     * O FIM da consulta e gravado no momento do agendamento.
     * Se amanha a duracao do procedimento mudar de 60 para 90 minutos,
     * a agenda de hoje continua consistente.
     */
    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConsulta status = StatusConsulta.AGENDADA;

    /*
     * Coluna GERADA pelo banco (nao setada por Java) que alimenta o indice
     * unico uk_consulta_dentista_horario. Vale data_hora enquanto a consulta
     * esta AGENDADA e NULL quando ela e cancelada ou realizada.
     *
     * Efeito: o banco recusa duas AGENDADAS do mesmo dentista no mesmo
     * instante, mas ignora canceladas/realizadas (varios NULL nao colidem num
     * indice unico no MySQL). insertable/updatable = false porque quem calcula
     * o valor e o proprio banco, a cada mudanca de status ou horario.
     */
    @Column(name = "chave_agenda", insertable = false, updatable = false,
            columnDefinition = "DATETIME AS "
                    + "(CASE WHEN status = 'AGENDADA' THEN data_hora ELSE NULL END) STORED")
    private LocalDateTime chaveAgenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelada_por", length = 20)
    private OrigemCancelamento canceladaPor;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    protected Consulta() {
    }

    public Consulta(Paciente paciente, Dentista dentista, Procedimento procedimento,
                    LocalDateTime dataHora, String observacoes) {
        if (dataHora.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel agendar uma consulta para uma data ou horario que ja passou.");
        }
        this.paciente = paciente;
        this.dentista = dentista;
        this.procedimento = procedimento;
        this.dataHora = dataHora;
        this.dataHoraFim = dataHora.plusMinutes(procedimento.getDuracaoMinutos());
        this.observacoes = observacoes;
        this.status = StatusConsulta.AGENDADA;
    }

    /** Cancelamento pode partir do paciente, do atendente ou do proprio dentista. */
    public void cancelar(OrigemCancelamento origem, String motivo) {
        if (status == StatusConsulta.REALIZADA) {
            throw new RegraDeNegocioException(
                    "Uma consulta ja realizada nao pode ser cancelada.");
        }
        if (status == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException("Esta consulta ja esta cancelada.");
        }
        this.status = StatusConsulta.CANCELADA;
        this.canceladaPor = origem;
        this.motivoCancelamento = motivo;
    }

    public void marcarComoRealizada() {
        garantirQueNaoEstaCancelada();
        this.status = StatusConsulta.REALIZADA;
    }

    /**
     * Remarca a consulta para outro horario e/ou outro dentista.
     *
     * Quem valida a disponibilidade do profissional e o AgendaService; aqui
     * fica apenas o que e regra da propria consulta: so remarca o que ainda
     * esta agendado, e o fim e sempre recalculado a partir da duracao do
     * procedimento.
     */
    public void remarcar(Dentista novoDentista, LocalDateTime novaDataHora) {
        if (status != StatusConsulta.AGENDADA) {
            throw new RegraDeNegocioException(
                    "Somente consultas agendadas podem ser remarcadas.");
        }
        if (novaDataHora.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel remarcar para uma data ou horario que ja passou.");
        }
        this.dentista = novoDentista;
        this.dataHora = novaDataHora;
        this.dataHoraFim = novaDataHora.plusMinutes(procedimento.getDuracaoMinutos());
    }

    public void garantirQueNaoEstaCancelada() {
        if (status == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException(
                    "Esta consulta foi cancelada e nao pode ser atendida.");
        }
    }

    public boolean estaAgendada() {
        return status == StatusConsulta.AGENDADA;
    }

    public Long getId() {
        return id;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public Dentista getDentista() {
        return dentista;
    }

    public Procedimento getProcedimento() {
        return procedimento;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public OrigemCancelamento getCanceladaPor() {
        return canceladaPor;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Consulta consulta)) {
            return false;
        }
        return id != null && id.equals(consulta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
