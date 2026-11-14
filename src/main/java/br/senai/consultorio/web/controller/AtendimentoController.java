package br.senai.consultorio.web.controller;

import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import br.senai.consultorio.dto.form.FinalizacaoForm;
import br.senai.consultorio.dto.form.UsoMaterialForm;
import br.senai.consultorio.service.AgendaService;
import br.senai.consultorio.service.AtendimentoService;
import br.senai.consultorio.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Execucao do atendimento: abrir a partir da consulta, lancar materiais e
 * finalizar. A emissao do comprovante fica no ComprovanteController.
 */
@Controller
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;
    private final ProdutoService produtoService;
    private final AgendaService agendaService;

    public AtendimentoController(AtendimentoService atendimentoService,
                                 ProdutoService produtoService,
                                 AgendaService agendaService) {
        this.atendimentoService = atendimentoService;
        this.produtoService = produtoService;
        this.agendaService = agendaService;
    }

    /**
     * Tela que pede a senha do dentista antes de abrir o atendimento.
     * O botao "Atender" da agenda leva para ca (GET); a confirmacao volta
     * pelo POST abaixo.
     */
    @GetMapping("/iniciar/{consultaId}")
    public String telaDeSenha(@PathVariable Long consultaId, Model model) {
        model.addAttribute("consulta", agendaService.buscar(consultaId));
        model.addAttribute("paginaAtiva", "agenda");
        return "atendimentos/confirmar";
    }

    @PostMapping("/iniciar/{consultaId}")
    public String iniciar(@PathVariable Long consultaId,
                          @RequestParam(required = false) String senha,
                          RedirectAttributes atributos) {
        try {
            var atendimento = atendimentoService.iniciar(consultaId, senha);
            return "redirect:/atendimentos/" + atendimento.id();
        } catch (RegraDeNegocioException excecao) {
            // Senha errada ou dentista sem senha: volta a tela pedindo de novo.
            atributos.addFlashAttribute("mensagemErro", excecao.getMessage());
            return "redirect:/atendimentos/iniciar/" + consultaId;
        }
    }

    @GetMapping("/{id}")
    public String executar(@PathVariable Long id, Model model) {
        model.addAttribute("atendimento", atendimentoService.buscar(id));
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("novoUso", new UsoMaterialForm());
        model.addAttribute("finalizacao", new FinalizacaoForm());
        model.addAttribute("historico", atendimentoService.historicoDoAtendimento(id));
        model.addAttribute("paginaAtiva", "agenda");
        return "atendimentos/executar";
    }

    @PostMapping("/{id}/materiais")
    public String registrarUso(@PathVariable Long id,
                               @Valid @ModelAttribute("novoUso") UsoMaterialForm form,
                               BindingResult erros,
                               Model model,
                               RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("atendimento", atendimentoService.buscar(id));
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("finalizacao", new FinalizacaoForm());
            model.addAttribute("paginaAtiva", "agenda");
            return "atendimentos/executar";
        }
        atendimentoService.registrarUso(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Material registrado.");
        return "redirect:/atendimentos/" + id;
    }

    @PostMapping("/{id}/materiais/{materialId}/excluir")
    public String removerMaterial(@PathVariable Long id,
                                  @PathVariable Long materialId,
                                  RedirectAttributes atributos) {
        atendimentoService.removerMaterial(id, materialId);
        atributos.addFlashAttribute("mensagemSucesso", "Material removido.");
        return "redirect:/atendimentos/" + id;
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(@PathVariable Long id,
                            @Valid @ModelAttribute("finalizacao") FinalizacaoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("atendimento", atendimentoService.buscar(id));
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("novoUso", new UsoMaterialForm());
            model.addAttribute("paginaAtiva", "agenda");
            return "atendimentos/executar";
        }
        atendimentoService.finalizar(id, form);
        atributos.addFlashAttribute("mensagemSucesso",
                "Atendimento finalizado. O comprovante ja pode ser emitido.");
        return "redirect:/atendimentos/" + id;
    }

    /**
     * Tela onde o dentista escreve a prescricao do receituario.
     * O PDF em si e gerado no POST /atendimentos/{id}/receituario,
     * tratado pelo ReceituarioController (mesmo padrao do comprovante).
     */
    @GetMapping("/{id}/receituario")
    public String telaDeReceituario(@PathVariable Long id, Model model) {
        model.addAttribute("atendimento", atendimentoService.buscar(id));
        model.addAttribute("paginaAtiva", "agenda");
        return "atendimentos/receituario";
    }
}
