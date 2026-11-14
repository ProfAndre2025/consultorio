package br.senai.consultorio.service;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Paciente;
import br.senai.consultorio.dto.form.PacienteForm;
import br.senai.consultorio.dto.view.PacienteView;
import br.senai.consultorio.mapper.PacienteMapper;
import br.senai.consultorio.repository.PacienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Regras de aplicacao do cadastro de pacientes.
 *
 * Tres decisoes de projeto que valem explicar em aula:
 *
 * 1. INJECAO POR CONSTRUTOR, com os campos final. Sem @Autowired em atributo.
 *    Assim a classe nao pode existir em estado incompleto e da para instanciar
 *    no teste sem subir o Spring.
 *
 * 2. O service depende de REPOSITORIOS, nunca de outro service. Se um service
 *    chamasse o outro, uma mudanca de regra se espalharia em cascata -- e
 *    dependencias circulares apareceriam rapido.
 *
 * 3. Nao existe interface PacienteService + classe PacienteServiceImpl.
 *    Interface com uma unica implementacao nao desacopla nada; so dobra o
 *    numero de arquivos. O desacoplamento real esta nos repositorios e nas
 *    portas de infraestrutura (ex.: GeradorComprovante).
 */
@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final PacienteMapper mapper;

    public PacienteService(PacienteRepository pacienteRepository,
                           PacienteMapper mapper) {
        this.pacienteRepository = pacienteRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PacienteView> listar(String termo, Boolean ativo, Pageable paginacao) {
        return pacienteRepository.buscarPorTermo(normalizar(termo), ativo, paginacao)
                .map(mapper::paraView);
    }

    @Transactional(readOnly = true)
    public List<PacienteView> listarTodos() {
        // Apenas ATIVOS: um paciente inativo nao deve aparecer para agendamento.
        return pacienteRepository.findByAtivoTrue(Sort.by("nome")).stream()
                .map(mapper::paraView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteView buscar(Long id) {
        return mapper.paraView(obrigatorio(id));
    }

    @Transactional(readOnly = true)
    public PacienteForm carregarParaEdicao(Long id) {
        return mapper.paraForm(obrigatorio(id));
    }

    @Transactional
    public PacienteView cadastrar(PacienteForm form) {
        String cpf = form.getCpf().trim();
        if (pacienteRepository.existsByCpf(cpf)) {
            throw new RegraDeNegocioException(
                    "Ja existe um paciente cadastrado com o CPF %s.".formatted(cpf));
        }
        Paciente paciente = mapper.paraEntidade(form);
        return mapper.paraView(pacienteRepository.save(paciente));
    }

    @Transactional
    public PacienteView atualizar(Long id, PacienteForm form) {
        Paciente paciente = obrigatorio(id);
        String cpf = form.getCpf().trim();
        if (pacienteRepository.existsByCpfAndIdNot(cpf, id)) {
            throw new RegraDeNegocioException(
                    "O CPF %s ja pertence a outro paciente.".formatted(cpf));
        }
        mapper.copiarParaEntidade(form, paciente);
        // Nao e preciso chamar save(): dentro da transacao o Hibernate detecta
        // a alteracao e grava sozinho (dirty checking).
        return mapper.paraView(paciente);
    }

    /**
     * LGPD: o cadastro nao e apagado. "Excluir" inativa o paciente -- ele sai
     * das listagens padrao, mas permanece no banco para consulta e pode ser
     * reativado. O historico de consultas fica preservado por consequencia.
     */
    @Transactional
    public void inativar(Long id) {
        obrigatorio(id).inativar();
    }

    @Transactional
    public void reativar(Long id) {
        obrigatorio(id).reativar();
    }

    /** Carrega a entidade ou explode com 404. Usado tambem por outros fluxos. */
    @Transactional(readOnly = true)
    public Paciente obrigatorio(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Paciente", id));
    }

    private String normalizar(String termo) {
        return StringUtils.hasText(termo) ? termo.trim() : null;
    }
}
