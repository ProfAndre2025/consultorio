package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.form.DentistaForm;
import br.senai.consultorio.dto.form.DisponibilidadeForm;
import br.senai.consultorio.service.DentistaService;
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
@RequestMapping("/dentistas")
public class DentistaController {

    private final DentistaService dentistaService;

    public DentistaController(DentistaService dentistaService) {
        this.dentistaService = dentistaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "false") boolean mostrarInativos,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        Pageable paginacao = PageRequest.of(pagina, 10, Sort.by("nome"));
        // mostrarInativos=false -> so ativos; true -> todos.
        Boolean ativo = mostrarInativos ? null : Boolean.TRUE;
        Page<?> resultado = dentistaService.listar(termo, ativo, paginacao);

        model.addAttribute("pagina", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("mostrarInativos", mostrarInativos);
        model.addAttribute("paginaAtiva", "dentistas");
        return "dentistas/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("dentista", new DentistaForm());
        model.addAttribute("edicao", false);
        model.addAttribute("paginaAtiva", "dentistas");
        return "dentistas/formulario";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("dentista") DentistaForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("edicao", false);
            model.addAttribute("paginaAtiva", "dentistas");
            return "dentistas/formulario";
        }
        var salvo = dentistaService.cadastrar(form);
        atributos.addFlashAttribute("mensagemSucesso",
                "Dentista cadastrado. Agora informe os dias e horarios de atendimento.");
        // Leva direto para a tela de agenda, onde as faixas de horario sao definidas.
        return "redirect:/dentistas/" + salvo.id() + "/agenda";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("dentista", dentistaService.carregarParaEdicao(id));
        model.addAttribute("dentistaId", id);
        model.addAttribute("edicao", true);
        model.addAttribute("paginaAtiva", "dentistas");
        return "dentistas/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("dentista") DentistaForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("dentistaId", id);
            model.addAttribute("edicao", true);
            model.addAttribute("paginaAtiva", "dentistas");
            return "dentistas/formulario";
        }
        dentistaService.atualizar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Dentista atualizado com sucesso.");
        return "redirect:/dentistas";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes atributos) {
        dentistaService.inativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Dentista inativado.");
        return "redirect:/dentistas";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Long id, RedirectAttributes atributos) {
        dentistaService.reativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Dentista reativado.");
        return "redirect:/dentistas?mostrarInativos=true";
    }

    // ----------------------------------------------------------------
    // Agenda do dentista (faixas de disponibilidade)
    // ----------------------------------------------------------------

    @GetMapping("/{id}/agenda")
    public String agenda(@PathVariable Long id, Model model) {
        model.addAttribute("dentista", dentistaService.buscarComAgenda(id));
        model.addAttribute("novaFaixa", new DisponibilidadeForm());
        model.addAttribute("paginaAtiva", "dentistas");
        return "dentistas/agenda";
    }

    @PostMapping("/{id}/agenda")
    public String adicionarFaixa(@PathVariable Long id,
                                 @Valid @ModelAttribute("novaFaixa") DisponibilidadeForm form,
                                 BindingResult erros,
                                 Model model,
                                 RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("dentista", dentistaService.buscarComAgenda(id));
            model.addAttribute("paginaAtiva", "dentistas");
            return "dentistas/agenda";
        }
        dentistaService.adicionarDisponibilidade(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Horario de atendimento adicionado.");
        return "redirect:/dentistas/" + id + "/agenda";
    }

    @PostMapping("/{id}/agenda/{faixaId}/excluir")
    public String removerFaixa(@PathVariable Long id,
                               @PathVariable Long faixaId,
                               RedirectAttributes atributos) {
        dentistaService.removerDisponibilidade(id, faixaId);
        atributos.addFlashAttribute("mensagemSucesso", "Horario removido.");
        return "redirect:/dentistas/" + id + "/agenda";
    }
}
