package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Produto;
import br.senai.consultorio.dto.form.ProdutoForm;
import br.senai.consultorio.dto.view.ProdutoView;
import org.springframework.stereotype.Component;

@Component
public class ProdutoMapper {

    public ProdutoView paraView(Produto produto) {
        return new ProdutoView(
                produto.getId(),
                produto.getNome(),
                produto.getUnidade(),
                produto.getPrecoUnitario(),
                produto.getEstoque(),
                produto.isAtivo());
    }

    public ProdutoForm paraForm(Produto produto) {
        ProdutoForm form = new ProdutoForm();
        form.setNome(produto.getNome());
        form.setUnidade(produto.getUnidade());
        form.setPrecoUnitario(produto.getPrecoUnitario());
        form.setEstoque(produto.getEstoque());
        return form;
    }

    public Produto paraEntidade(ProdutoForm form) {
        return new Produto(
                form.getNome().trim(),
                form.getUnidade().trim(),
                form.getPrecoUnitario(),
                form.getEstoque());
    }

    public void copiarParaEntidade(ProdutoForm form, Produto produto) {
        produto.setNome(form.getNome().trim());
        produto.setUnidade(form.getUnidade().trim());
        produto.setPrecoUnitario(form.getPrecoUnitario());
        produto.setEstoque(form.getEstoque());
    }
}
