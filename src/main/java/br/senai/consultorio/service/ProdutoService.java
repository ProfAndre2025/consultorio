package br.senai.consultorio.service;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.domain.model.Produto;
import br.senai.consultorio.dto.form.ProdutoForm;
import br.senai.consultorio.dto.view.ProdutoView;
import br.senai.consultorio.mapper.ProdutoMapper;
import br.senai.consultorio.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper mapper;

    public ProdutoService(ProdutoRepository produtoRepository, ProdutoMapper mapper) {
        this.produtoRepository = produtoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoView> listar(String termo, Boolean ativo, Pageable paginacao) {
        return produtoRepository.buscarPorTermo(normalizar(termo), ativo, paginacao)
                .map(mapper::paraView);
    }

    @Transactional(readOnly = true)
    public List<ProdutoView> listarTodos() {
        // Apenas ATIVOS: material inativo nao aparece para vincular/usar.
        return produtoRepository.findByAtivoTrue(Sort.by("nome")).stream()
                .map(mapper::paraView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoForm carregarParaEdicao(Long id) {
        return mapper.paraForm(obrigatorio(id));
    }

    @Transactional
    public ProdutoView cadastrar(ProdutoForm form) {
        String nome = form.getNome().trim();
        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException(
                    "Ja existe um material chamado '%s'.".formatted(nome));
        }
        return mapper.paraView(produtoRepository.save(mapper.paraEntidade(form)));
    }

    @Transactional
    public ProdutoView atualizar(Long id, ProdutoForm form) {
        Produto produto = obrigatorio(id);
        String nome = form.getNome().trim();
        if (produtoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException(
                    "Ja existe outro material chamado '%s'.".formatted(nome));
        }
        mapper.copiarParaEntidade(form, produto);
        return mapper.paraView(produto);
    }

    /**
     * LGPD: material nao e apagado, e inativado -- sai das listagens mas fica
     * no banco. Como nada e removido, as antigas travas (usado em atendimento,
     * vinculado a procedimento) deixam de ser necessarias: o historico e os
     * vinculos continuam integros.
     */
    @Transactional
    public void inativar(Long id) {
        obrigatorio(id).inativar();
    }

    @Transactional
    public void reativar(Long id) {
        obrigatorio(id).reativar();
    }

    @Transactional(readOnly = true)
    public Produto obrigatorio(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Material", id));
    }

    private String normalizar(String termo) {
        return StringUtils.hasText(termo) ? termo.trim() : null;
    }
}
