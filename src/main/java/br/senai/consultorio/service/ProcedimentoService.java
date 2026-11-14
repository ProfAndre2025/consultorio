package br.senai.consultorio.service;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Procedimento;
import br.senai.consultorio.domain.model.Produto;
import br.senai.consultorio.dto.form.ProcedimentoForm;
import br.senai.consultorio.dto.form.VincularMaterialForm;
import br.senai.consultorio.dto.view.ProcedimentoResumoView;
import br.senai.consultorio.dto.view.ProcedimentoView;
import br.senai.consultorio.mapper.ProcedimentoMapper;
import br.senai.consultorio.repository.ProcedimentoRepository;
import br.senai.consultorio.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;
    private final ProdutoRepository produtoRepository;
    private final ProcedimentoMapper mapper;

    public ProcedimentoService(ProcedimentoRepository procedimentoRepository,
                               ProdutoRepository produtoRepository,
                               ProcedimentoMapper mapper) {
        this.procedimentoRepository = procedimentoRepository;
        this.produtoRepository = produtoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ProcedimentoResumoView> listar(String termo, Boolean ativo, Pageable paginacao) {
        // Resumo, e nao ProcedimentoView: montar a view completa exigiria
        // carregar os materiais de cada linha -- uma consulta por procedimento.
        return procedimentoRepository.buscarPorTermo(normalizar(termo), ativo, paginacao)
                .map(mapper::paraResumo);
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResumoView> listarTodos() {
        // Apenas ATIVOS: procedimento inativo nao aparece para agendar.
        return procedimentoRepository.findByAtivoTrue(Sort.by("nome")).stream()
                .map(mapper::paraResumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcedimentoView buscarComMateriais(Long id) {
        return mapper.paraView(carregarComMateriais(id));
    }

    @Transactional(readOnly = true)
    public ProcedimentoForm carregarParaEdicao(Long id) {
        return mapper.paraForm(obrigatorio(id));
    }

    @Transactional
    public ProcedimentoView cadastrar(ProcedimentoForm form) {
        String nome = form.getNome().trim();
        if (procedimentoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException(
                    "Ja existe um procedimento chamado '%s'.".formatted(nome));
        }
        return mapper.paraView(procedimentoRepository.save(mapper.paraEntidade(form)));
    }

    @Transactional
    public ProcedimentoView atualizar(Long id, ProcedimentoForm form) {
        Procedimento procedimento = carregarComMateriais(id);
        String nome = form.getNome().trim();
        if (procedimentoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException(
                    "Ja existe outro procedimento chamado '%s'.".formatted(nome));
        }
        mapper.copiarParaEntidade(form, procedimento);
        return mapper.paraView(procedimento);
    }

    /** LGPD: procedimento nao e apagado, e inativado. O historico e preservado. */
    @Transactional
    public void inativar(Long id) {
        obrigatorio(id).inativar();
    }

    @Transactional
    public void reativar(Long id) {
        obrigatorio(id).reativar();
    }

    @Transactional
    public ProcedimentoView vincularMaterial(Long procedimentoId, VincularMaterialForm form) {
        Procedimento procedimento = carregarComMateriais(procedimentoId);
        Produto produto = produtoRepository.findById(form.getProdutoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Material", form.getProdutoId()));

        procedimento.vincularMaterial(produto, form.getQuantidade());
        return mapper.paraView(procedimento);
    }

    @Transactional
    public ProcedimentoView desvincularMaterial(Long procedimentoId, Long vinculoId) {
        Procedimento procedimento = carregarComMateriais(procedimentoId);
        procedimento.desvincularMaterial(vinculoId);
        return mapper.paraView(procedimento);
    }

    @Transactional(readOnly = true)
    public Procedimento obrigatorio(Long id) {
        return procedimentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Procedimento", id));
    }

    /** Usa join fetch: evita o problema N+1 ao somar o preco dos materiais. */
    private Procedimento carregarComMateriais(Long id) {
        return procedimentoRepository.buscarComMateriais(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Procedimento", id));
    }

    private String normalizar(String termo) {
        return StringUtils.hasText(termo) ? termo.trim() : null;
    }
}
