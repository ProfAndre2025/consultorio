package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Procedimento;
import br.senai.consultorio.domain.model.ProcedimentoProduto;
import br.senai.consultorio.dto.form.ProcedimentoForm;
import br.senai.consultorio.dto.view.MaterialPadraoView;
import br.senai.consultorio.dto.view.ProcedimentoResumoView;
import br.senai.consultorio.dto.view.ProcedimentoView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ProcedimentoMapper {

    public ProcedimentoView paraView(Procedimento procedimento) {
        List<MaterialPadraoView> materiais = procedimento.getMateriais().stream()
                .sorted(Comparator.comparing((ProcedimentoProduto m) -> m.getProduto().getNome()))
                .map(this::paraView)
                .toList();

        return new ProcedimentoView(
                procedimento.getId(),
                procedimento.getNome(),
                procedimento.getDescricao(),
                procedimento.getPrecoMaoObra(),
                procedimento.getDuracaoMinutos(),
                procedimento.calcularPrecoSugerido(),
                materiais);
    }

    /** Versao sem materiais, para listagens. Nao toca na colecao LAZY. */
    public ProcedimentoResumoView paraResumo(Procedimento procedimento) {
        return new ProcedimentoResumoView(
                procedimento.getId(),
                procedimento.getNome(),
                procedimento.getDescricao(),
                procedimento.getPrecoMaoObra(),
                procedimento.getDuracaoMinutos(),
                procedimento.isAtivo());
    }

    public MaterialPadraoView paraView(ProcedimentoProduto vinculo) {
        return new MaterialPadraoView(
                vinculo.getId(),
                vinculo.getProduto().getId(),
                vinculo.getProduto().getNome(),
                vinculo.getProduto().getUnidade(),
                vinculo.getQuantidade(),
                vinculo.getProduto().getPrecoUnitario(),
                vinculo.calcularSubtotal());
    }

    public ProcedimentoForm paraForm(Procedimento procedimento) {
        ProcedimentoForm form = new ProcedimentoForm();
        form.setNome(procedimento.getNome());
        form.setDescricao(procedimento.getDescricao());
        form.setPrecoMaoObra(procedimento.getPrecoMaoObra());
        form.setDuracaoMinutos(procedimento.getDuracaoMinutos());
        return form;
    }

    public Procedimento paraEntidade(ProcedimentoForm form) {
        Procedimento procedimento = new Procedimento(
                form.getNome().trim(), form.getPrecoMaoObra(), form.getDuracaoMinutos());
        procedimento.setDescricao(form.getDescricao());
        return procedimento;
    }

    public void copiarParaEntidade(ProcedimentoForm form, Procedimento procedimento) {
        procedimento.setNome(form.getNome().trim());
        procedimento.setDescricao(form.getDescricao());
        procedimento.setPrecoMaoObra(form.getPrecoMaoObra());
        procedimento.setDuracaoMinutos(form.getDuracaoMinutos());
    }
}
