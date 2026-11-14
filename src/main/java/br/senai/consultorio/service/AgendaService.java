package br.senai.consultorio.service;

import br.senai.consultorio.domain.enums.StatusConsulta;
import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Atendimento;
import br.senai.consultorio.domain.model.Consulta;
import br.senai.consultorio.domain.model.Dentista;
import br.senai.consultorio.domain.model.Paciente;
import br.senai.consultorio.domain.model.Procedimento;
import br.senai.consultorio.dto.form.AgendamentoForm;
import br.senai.consultorio.dto.form.CancelamentoForm;
import br.senai.consultorio.dto.form.ReagendamentoForm;
import br.senai.consultorio.dto.view.ConsultaView;
import br.senai.consultorio.dto.view.DentistaDisponivelView;
import br.senai.consultorio.mapper.ConsultaMapper;
import br.senai.consultorio.mapper.DentistaMapper;
import br.senai.consultorio.repository.AtendimentoRepository;
import br.senai.consultorio.repository.ConsultaRepository;
import br.senai.consultorio.repository.DentistaRepository;
import br.senai.consultorio.repository.PacienteRepository;
import br.senai.consultorio.repository.ProcedimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Coracao do sistema: agendamento e cancelamento de consultas.
 */
@Service
public class AgendaService {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final ConsultaMapper consultaMapper;
    private final DentistaMapper dentistaMapper;

    public AgendaService(ConsultaRepository consultaRepository,
                         PacienteRepository pacienteRepository,
                         DentistaRepository dentistaRepository,
                         ProcedimentoRepository procedimentoRepository,
                         AtendimentoRepository atendimentoRepository,
                         ConsultaMapper consultaMapper,
                         DentistaMapper dentistaMapper) {
        this.consultaRepository = consultaRepository;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.consultaMapper = consultaMapper;
        this.dentistaMapper = dentistaMapper;
    }

    /**
     * Passo 1 da tela de agendamento: quem esta livre neste horario?
     *
     * A janela e calculada a partir da DURACAO DO PROCEDIMENTO -- nao adianta o
     * dentista atender ate as 12h se o procedimento de 30 min comeca as 11h45.
     */
    @Transactional(readOnly = true)
    public List<DentistaDisponivelView> dentistasDisponiveis(Long procedimentoId,
                                                             LocalDateTime dataHora) {
        Procedimento procedimento = procedimentoObrigatorio(procedimentoId);

        if (dataHora.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel agendar em data ou horario que ja passou.");
        }

        LocalDateTime fim = dataHora.plusMinutes(procedimento.getDuracaoMinutos());

        return dentistaRepository.buscarDisponiveisEntre(dataHora, fim).stream()
                .map(dentistaMapper::paraDisponivelView)
                .toList();
    }

    /**
     * Passo 2: grava a consulta.
     *
     * A disponibilidade e conferida DE NOVO aqui. Entre o clique em "ver
     * dentistas" e o clique em "confirmar" outro atendente pode ter agendado
     * o mesmo horario -- confiar na tela seria confiar no cliente.
     */
    @Transactional
    public ConsultaView agendar(AgendamentoForm form) {
        if (form.getDentistaId() == null) {
            throw new RegraDeNegocioException(
                    "Consulte os dentistas disponiveis e selecione um profissional.");
        }

        Paciente paciente = pacienteRepository.findById(form.getPacienteId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Paciente", form.getPacienteId()));
        Procedimento procedimento = procedimentoObrigatorio(form.getProcedimentoId());
        Dentista dentista = dentistaRepository.findById(form.getDentistaId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", form.getDentistaId()));

        if (!dentista.isAtivo()) {
            throw new RegraDeNegocioException(
                    "%s esta inativo e nao pode receber agendamentos.".formatted(dentista.getNome()));
        }
        // LGPD/soft delete: registros inativos nao entram em novos agendamentos.
        if (!paciente.isAtivo()) {
            throw new RegraDeNegocioException(
                    "%s esta inativo. Reative o cadastro para agendar."
                            .formatted(paciente.getNome()));
        }
        if (!procedimento.isAtivo()) {
            throw new RegraDeNegocioException(
                    "O procedimento '%s' esta inativo e nao pode ser agendado."
                            .formatted(procedimento.getNome()));
        }

        LocalDateTime inicio = form.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(procedimento.getDuracaoMinutos());

        boolean atendeNesteHorario = dentistaRepository
                .buscarDisponiveisEntre(inicio, fim).stream()
                .anyMatch(d -> d.getId().equals(dentista.getId()));

        if (!atendeNesteHorario) {
            throw new RegraDeNegocioException(
                    "%s nao esta disponivel neste horario. Verifique os dias de "
                            .formatted(dentista.getNome())
                            + "atendimento cadastrados e as consultas ja marcadas.");
        }

        if (consultaRepository.existeConflito(dentista.getId(), inicio, fim)) {
            throw new RegraDeNegocioException(
                    "Outra consulta acabou de ser marcada para este dentista neste horario.");
        }

        Consulta consulta = new Consulta(
                paciente, dentista, procedimento, inicio, form.getObservacoes());

        try {
            // O flush força o INSERT agora, ainda dentro do try, para que uma
            // eventual violação do índice único (corrida entre dois atendentes)
            // vire mensagem clara em vez de erro 500 no commit.
            Consulta salva = consultaRepository.saveAndFlush(consulta);
            return consultaMapper.paraView(salva);
        } catch (org.springframework.dao.DataIntegrityViolationException colisao) {
            throw new RegraDeNegocioException(
                    "%s acabou de receber outra consulta neste mesmo horario. "
                            .formatted(dentista.getNome())
                            + "Escolha outro horario.");
        }
    }

    /**
     * Remarca a consulta: nova data/horario e, se preciso, outro dentista.
     *
     * A disponibilidade e conferida em dois passos, e nao pelo
     * buscarDisponiveisEntre usado no agendamento. O motivo: aquela consulta
     * exclui quem ja tem compromisso no horario -- e a propria consulta que
     * estamos movendo apareceria como conflito consigo mesma, impedindo, por
     * exemplo, trocar so o dentista mantendo o horario.
     */
    @Transactional
    public ConsultaView remarcar(Long consultaId, ReagendamentoForm form) {
        Consulta consulta = consultaRepository.buscarCompleta(consultaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Consulta", consultaId));

        Dentista dentista = dentistaRepository.findById(form.getDentistaId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", form.getDentistaId()));

        if (!dentista.isAtivo()) {
            throw new RegraDeNegocioException(
                    "%s esta inativo e nao pode receber agendamentos.".formatted(dentista.getNome()));
        }

        LocalDateTime inicio = form.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(consulta.getProcedimento().getDuracaoMinutos());

        // 1) o profissional atende neste dia da semana e nesta faixa?
        boolean atende = dentistaRepository.atendeNaFaixa(
                dentista.getId(), inicio.getDayOfWeek(), inicio.toLocalTime(), fim.toLocalTime());
        if (!atende) {
            throw new RegraDeNegocioException(
                    "%s nao atende neste dia e horario. Verifique as faixas cadastradas."
                            .formatted(dentista.getNome()));
        }

        // 2) ha outra consulta ocupando a janela? (ignorando esta)
        if (consultaRepository.existeConflitoExcluindo(dentista.getId(), inicio, fim, consultaId)) {
            throw new RegraDeNegocioException(
                    "%s ja tem outra consulta marcada neste horario.".formatted(dentista.getNome()));
        }

        consulta.remarcar(dentista, inicio);
        try {
            // Mesmo motivo do agendar: flush dentro do try para transformar a
            // colisao do indice unico em mensagem amigavel.
            consultaRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException colisao) {
            throw new RegraDeNegocioException(
                    "%s acabou de receber outra consulta neste mesmo horario. "
                            .formatted(dentista.getNome())
                            + "Escolha outro horario.");
        }
        return consultaMapper.paraView(consulta);
    }

    /**
     * Consultas AGENDADAS para hoje, em ordem de horario.
     * Alimenta o painel inicial, onde a recepcao acompanha o dia.
     */
    @Transactional(readOnly = true)
    public List<ConsultaView> agendaDeHoje() {
        LocalDate hoje = LocalDate.now();

        return consultaRepository.buscarAgenda(
                        hoje.atStartOfDay(),
                        hoje.atTime(23, 59, 59),
                        StatusConsulta.AGENDADA,
                        null,
                        PageRequest.of(0, 50))
                .map(consultaMapper::paraView)
                .getContent();
    }

    /** Cancelamento pode partir do paciente, do atendente ou do proprio dentista. */
    @Transactional
    public ConsultaView cancelar(Long consultaId, CancelamentoForm form) {
        Consulta consulta = consultaRepository.buscarCompleta(consultaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Consulta", consultaId));

        // Quem decide se pode cancelar e a propria entidade.
        consulta.cancelar(form.getCanceladaPor(), form.getMotivo());

        return consultaMapper.paraView(consulta);
    }

    @Transactional(readOnly = true)
    public Page<ConsultaView> listar(LocalDate inicio, LocalDate fim,
                                     StatusConsulta status, String termo,
                                     Pageable paginacao) {
        LocalDateTime de = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime ate = fim != null ? fim.atTime(23, 59, 59) : null;

        Page<Consulta> pagina =
                consultaRepository.buscarAgenda(de, ate, status, normalizar(termo), paginacao);

        // Uma unica consulta traz os atendimentos de TODA a pagina.
        Map<Long, Atendimento> porConsulta = atendimentoRepository
                .findByConsultaIdIn(pagina.map(Consulta::getId).toList())
                .stream()
                .collect(Collectors.toMap(a -> a.getConsulta().getId(), a -> a));

        return pagina.map(consulta -> consultaMapper.paraView(
                consulta, Optional.ofNullable(porConsulta.get(consulta.getId()))));
    }

    @Transactional(readOnly = true)
    public ConsultaView buscar(Long consultaId) {
        Consulta consulta = consultaRepository.buscarCompleta(consultaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Consulta", consultaId));
        return consultaMapper.paraView(
                consulta, atendimentoRepository.findByConsultaId(consultaId));
    }

    private Procedimento procedimentoObrigatorio(Long id) {
        return procedimentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Procedimento", id));
    }

    private String normalizar(String termo) {
        return StringUtils.hasText(termo) ? termo.trim() : null;
    }
}
