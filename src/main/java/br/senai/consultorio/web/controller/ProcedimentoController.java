package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.form.ProcedimentoForm;
import br.senai.consultorio.dto.form.VincularMaterialForm;
import br.senai.consultorio.service.ProcedimentoService;
import br.senai.consultorio.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoService procedimentoService;
    private final ProdutoService produtoService;

    public ProcedimentoController(ProcedimentoService procedimentoService,
                                  ProdutoService produtoService) {
        this.procedimentoService = procedimentoService;
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "false") boolean mostrarInativos,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        Pageable paginacao = PageRequest.of(pagina, 10, Sort.by("nome"));
        Boolean ativo = mostrarInativos ? null : Boolean.TRUE;
        Page<?> resultado = procedimentoService.listar(termo, ativo, paginacao);

        model.addAttribute("pagina", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("mostrarInativos", mostrarInativos);
        model.addAttribute("paginaAtiva", "procedimentos");
        return "procedimentos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("procedimento", new ProcedimentoForm());
        model.addAttribute("edicao", false);
        model.addAttribute("paginaAtiva", "procedimentos");
        return "procedimentos/formulario";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("procedimento") ProcedimentoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("edicao", false);
            model.addAttribute("paginaAtiva", "procedimentos");
            return "procedimentos/formulario";
        }
        var salvo = procedimentoService.cadastrar(form);
        atributos.addFlashAttribute("mensagemSucesso",
                "Procedimento cadastrado. Agora vincule os materiais.");
        return "redirect:/procedimentos/" + salvo.id() + "/materiais";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("procedimento", procedimentoService.carregarParaEdicao(id));
        model.addAttribute("procedimentoId", id);
        model.addAttribute("edicao", true);
        model.addAttribute("paginaAtiva", "procedimentos");
        return "procedimentos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("procedimento") ProcedimentoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("procedimentoId", id);
            model.addAttribute("edicao", true);
            model.addAttribute("paginaAtiva", "procedimentos");
            return "procedimentos/formulario";
        }
        procedimentoService.atualizar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Procedimento atualizado com sucesso.");
        return "redirect:/procedimentos";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes atributos) {
        procedimentoService.inativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Procedimento inativado.");
        return "redirect:/procedimentos";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Long id, RedirectAttributes atributos) {
        procedimentoService.reativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Procedimento reativado.");
        return "redirect:/procedimentos?mostrarInativos=true";
    }

    // ----------------------------------------------------------------
    // Materiais padrao do procedimento
    // ----------------------------------------------------------------

    @GetMapping("/{id}/materiais")
    public String materiais(@PathVariable Long id, Model model) {
        model.addAttribute("procedimento", procedimentoService.buscarComMateriais(id));
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("novoMaterial", new VincularMaterialForm());
        model.addAttribute("paginaAtiva", "procedimentos");
        return "procedimentos/materiais";
    }

    @PostMapping("/{id}/materiais")
    public String vincularMaterial(@PathVariable Long id,
                                   @Valid @ModelAttribute("novoMaterial") VincularMaterialForm form,
                                   BindingResult erros,
                                   Model model,
                                   RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("procedimento", procedimentoService.buscarComMateriais(id));
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("paginaAtiva", "procedimentos");
            return "procedimentos/materiais";
        }
        procedimentoService.vincularMaterial(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Material vinculado ao procedimento.");
        return "redirect:/procedimentos/" + id + "/materiais";
    }

    @PostMapping("/{id}/materiais/{vinculoId}/excluir")
    public String desvincularMaterial(@PathVariable Long id,
                                      @PathVariable Long vinculoId,
                                      RedirectAttributes atributos) {
        procedimentoService.desvincularMaterial(id, vinculoId);
        atributos.addFlashAttribute("mensagemSucesso", "Material desvinculado.");
        return "redirect:/procedimentos/" + id + "/materiais";
    }
}
