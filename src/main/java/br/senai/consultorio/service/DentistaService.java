package br.senai.consultorio.service;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Dentista;
import br.senai.consultorio.domain.model.Disponibilidade;
import br.senai.consultorio.dto.form.DentistaForm;
import br.senai.consultorio.dto.form.DisponibilidadeForm;
import br.senai.consultorio.dto.view.DentistaView;
import br.senai.consultorio.mapper.DentistaMapper;
import br.senai.consultorio.repository.DentistaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DentistaService {

    private final DentistaRepository dentistaRepository;
    private final DentistaMapper mapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DentistaService(DentistaRepository dentistaRepository,
                           DentistaMapper mapper,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.dentistaRepository = dentistaRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<DentistaView> listar(String termo, boolean somenteAtivos, Pageable paginacao) {
        // O repositorio espera Boolean ANULAVEL: null = "nao filtrar por situacao".
        // Passar false direto traria apenas os INATIVOS.
        Boolean filtroAtivo = somenteAtivos ? Boolean.TRUE : null;

        return dentistaRepository.buscarPorTermo(normalizar(termo), filtroAtivo, paginacao)
                .map(mapper::paraViewResumida);
    }

    @Transactional(readOnly = true)
    public List<DentistaView> listarAtivos() {
        return dentistaRepository.findAll(Sort.by("nome")).stream()
                .filter(Dentista::isAtivo)
                .map(mapper::paraViewResumida)
                .toList();
    }

    /** Traz o dentista JUNTO com as faixas de horario, em uma unica consulta. */
    @Transactional(readOnly = true)
    public DentistaView buscarComAgenda(Long id) {
        Dentista dentista = dentistaRepository.buscarComDisponibilidades(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", id));
        return mapper.paraView(dentista);
    }

    @Transactional(readOnly = true)
    public DentistaForm carregarParaEdicao(Long id) {
        return mapper.paraForm(obrigatorio(id));
    }

    @Transactional
    public DentistaView cadastrar(DentistaForm form) {
        String cro = form.getCro().trim();
        if (dentistaRepository.existsByCro(cro)) {
            throw new RegraDeNegocioException(
                    "Ja existe um dentista cadastrado com o CRO %s.".formatted(cro));
        }
        Dentista dentista = mapper.paraEntidade(form);
        // Senha e opcional no cadastro; so grava (com hash) se veio preenchida.
        if (form.getSenha() != null && !form.getSenha().isBlank()) {
            dentista.definirSenha(form.getSenha(), passwordEncoder);
        }
        return mapper.paraViewResumida(dentistaRepository.save(dentista));
    }

    @Transactional
    public DentistaView atualizar(Long id, DentistaForm form) {
        Dentista dentista = obrigatorio(id);
        String cro = form.getCro().trim();
        if (dentistaRepository.existsByCroAndIdNot(cro, id)) {
            throw new RegraDeNegocioException(
                    "O CRO %s ja pertence a outro dentista.".formatted(cro));
        }
        mapper.copiarParaEntidade(form, dentista);
        // Campo de senha vazio na edicao = manter a atual. So troca se digitada.
        if (form.getSenha() != null && !form.getSenha().isBlank()) {
            dentista.definirSenha(form.getSenha(), passwordEncoder);
        }
        return mapper.paraViewResumida(dentista);
    }

    /**
     * LGPD: dentista nao e apagado, e inativado -- sai da agenda e das
     * listagens padrao, mas permanece no banco (o historico de atendimentos
     * que ele assinou fica preservado) e pode ser reativado.
     */
    @Transactional
    public void inativar(Long id) {
        obrigatorio(id).inativar();
    }

    @Transactional
    public void reativar(Long id) {
        obrigatorio(id).reativar();
    }

    // ----------------------------------------------------------------
    // Disponibilidade
    //
    // Nao existe DisponibilidadeService nem DisponibilidadeRepository: a faixa
    // de horario e parte do agregado Dentista e so se altera atraves dele.
    // Isso mantem a regra de sobreposicao em UM lugar so.
    // ----------------------------------------------------------------

    @Transactional
    public DentistaView adicionarDisponibilidade(Long dentistaId, DisponibilidadeForm form) {
        Dentista dentista = dentistaRepository.buscarComDisponibilidades(dentistaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", dentistaId));

        dentista.adicionarDisponibilidade(new Disponibilidade(
                form.getDiaSemana(), form.getHoraInicio(), form.getHoraFim()));

        return mapper.paraView(dentista);
    }

    @Transactional
    public DentistaView removerDisponibilidade(Long dentistaId, Long disponibilidadeId) {
        Dentista dentista = dentistaRepository.buscarComDisponibilidades(dentistaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", dentistaId));

        dentista.removerDisponibilidade(disponibilidadeId);
        return mapper.paraView(dentista);
    }

    @Transactional(readOnly = true)
    public Dentista obrigatorio(Long id) {
        return dentistaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Dentista", id));
    }

    private String normalizar(String termo) {
        return StringUtils.hasText(termo) ? termo.trim() : null;
    }
}
