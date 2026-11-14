package br.senai.consultorio.service;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Atendimento;
import br.senai.consultorio.domain.model.AtendimentoMaterial;
import br.senai.consultorio.domain.model.Consulta;
import br.senai.consultorio.domain.model.Dentista;
import br.senai.consultorio.domain.model.ProcedimentoProduto;
import br.senai.consultorio.domain.model.Produto;
import br.senai.consultorio.dto.form.FinalizacaoForm;
import br.senai.consultorio.dto.form.UsoMaterialForm;
import br.senai.consultorio.dto.view.AtendimentoView;
import br.senai.consultorio.dto.view.HistoricoItemView;
import br.senai.consultorio.mapper.AtendimentoMapper;
import br.senai.consultorio.repository.AtendimentoRepository;
import br.senai.consultorio.repository.ConsultaRepository;
import br.senai.consultorio.repository.ProcedimentoRepository;
import br.senai.consultorio.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Execucao do procedimento: materiais utilizados, fechamento e baixa de estoque.
 */
@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final ConsultaRepository consultaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final ProdutoRepository produtoRepository;
    private final AtendimentoMapper mapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AtendimentoService(AtendimentoRepository atendimentoRepository,
                              ConsultaRepository consultaRepository,
                              ProcedimentoRepository procedimentoRepository,
                              ProdutoRepository produtoRepository,
                              AtendimentoMapper mapper,
                              org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.atendimentoRepository = atendimentoRepository;
        this.consultaRepository = consultaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.produtoRepository = produtoRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Abre o atendimento e ja lanca os materiais PADRAO do procedimento.
     *
     * E idempotente: chamar duas vezes na mesma consulta devolve o mesmo
     * atendimento, sem duplicar os materiais.
     */
    @Transactional
    public AtendimentoView iniciar(Long consultaId, String senhaDentista) {
        Atendimento existente = atendimentoRepository.findByConsultaId(consultaId).orElse(null);
        if (existente != null) {
            return mapper.paraView(existente);
        }

        Consulta consulta = consultaRepository.buscarCompleta(consultaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Consulta", consultaId));

        // O atendimento so comeca com a senha do dentista responsavel. E o
        // "de acordo" do profissional: quem assina o procedimento e ele.
        Dentista dentista = consulta.getDentista();
        if (!dentista.possuiSenha()) {
            throw new RegraDeNegocioException(
                    "%s ainda nao tem senha de atendimento cadastrada. "
                            .formatted(dentista.getNome())
                            + "Defina a senha no cadastro do dentista antes de iniciar.");
        }
        if (!dentista.senhaConfere(senhaDentista, passwordEncoder)) {
            throw new RegraDeNegocioException("Senha do dentista incorreta.");
        }

        // O construtor recusa consulta cancelada.
        Atendimento atendimento = new Atendimento(consulta);

        // join fetch para nao disparar uma consulta por material (N+1).
        procedimentoRepository.buscarComMateriais(consulta.getProcedimento().getId())
                .ifPresent(procedimento -> {
                    for (ProcedimentoProduto padrao : procedimento.getMateriais()) {
                        atendimento.registrarUso(padrao.getProduto(), padrao.getQuantidade());
                    }
                });

        return mapper.paraView(atendimentoRepository.save(atendimento));
    }

    /**
     * Registra um material realmente utilizado.
     *
     * Quem decide se o material e "adicional" e a propria entidade Atendimento,
     * comparando com a lista padrao do procedimento. O service nao repete
     * essa regra.
     */
    @Transactional
    public AtendimentoView registrarUso(Long atendimentoId, UsoMaterialForm form) {
        Atendimento atendimento = carregarCompleto(atendimentoId);
        Produto produto = produtoRepository.findById(form.getProdutoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Material", form.getProdutoId()));

        atendimento.registrarUso(produto, form.getQuantidade());
        return mapper.paraView(atendimento);
    }

    @Transactional
    public AtendimentoView removerMaterial(Long atendimentoId, Long materialId) {
        Atendimento atendimento = carregarCompleto(atendimentoId);
        atendimento.removerMaterial(materialId);
        return mapper.paraView(atendimento);
    }

    /**
     * Fecha o atendimento.
     *
     * Tudo o que acontece aqui esta em UMA transacao: se um unico material
     * estiver sem estoque, nada e gravado -- nem a baixa dos anteriores, nem a
     * mudanca de status da consulta. Era exatamente isso que faltava na versao
     * Python, onde cada comando ia para o banco separadamente.
     */
    @Transactional
    public AtendimentoView finalizar(Long atendimentoId, FinalizacaoForm form) {
        Atendimento atendimento = carregarCompleto(atendimentoId);
        atendimento.garantirQueEstaAberto();

        for (AtendimentoMaterial material : atendimento.getMateriais()) {
            material.getProduto().baixarEstoque(material.getQuantidade());
        }

        atendimento.finalizar(form.getObservacoes());
        return mapper.paraView(atendimento);
    }

    @Transactional(readOnly = true)
    public AtendimentoView buscar(Long atendimentoId) {
        return mapper.paraView(carregarCompleto(atendimentoId));
    }

    /** Carrega o atendimento com consulta, paciente, dentista e materiais de uma vez. */
    @Transactional(readOnly = true)
    public Atendimento carregarCompleto(Long atendimentoId) {
        return atendimentoRepository.buscarCompleto(atendimentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Atendimento", atendimentoId));
    }

    /**
     * Historico clinico do paciente: atendimentos realizados, do mais recente
     * para o mais antigo. Alimenta a ficha do paciente e a tela de atendimento,
     * para o dentista consultar no retorno o que ja foi feito.
     */
    @Transactional(readOnly = true)
    public List<HistoricoItemView> historicoDoPaciente(Long pacienteId) {
        return atendimentoRepository.buscarHistoricoDoPaciente(pacienteId).stream()
                .map(this::paraHistorico)
                .toList();
    }

    /**
     * Historico do paciente DESTE atendimento, para a tela de execucao mostrar
     * o passado clinico durante o retorno. Exclui o proprio atendimento aberto
     * -- ele ainda esta em curso e nao e "historico".
     */
    @Transactional(readOnly = true)
    public List<HistoricoItemView> historicoDoAtendimento(Long atendimentoId) {
        Atendimento atual = carregarCompleto(atendimentoId);
        Long pacienteId = atual.getConsulta().getPaciente().getId();
        return atendimentoRepository.buscarHistoricoDoPaciente(pacienteId).stream()
                .filter(a -> !a.getId().equals(atendimentoId))
                .map(this::paraHistorico)
                .toList();
    }

    private HistoricoItemView paraHistorico(Atendimento atendimento) {
        // Materiais como texto ("Resina composta A2 - 2,00 g"): o historico
        // mostra O QUE foi usado, sem precos.
        List<String> materiais = atendimento.getMateriais().stream()
                .map(m -> "%s - %s %s".formatted(
                        m.getNomeProduto(),
                        m.getQuantidade().stripTrailingZeros().toPlainString(),
                        m.getUnidade()))
                .toList();

        var consulta = atendimento.getConsulta();
        return new HistoricoItemView(
                atendimento.getId(),
                atendimento.getRealizadoEm(),
                consulta.getProcedimento().getNome(),
                consulta.getDentista().getNome(),
                consulta.getDentista().getCro(),
                materiais,
                atendimento.getObservacoes());
    }
}
