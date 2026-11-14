package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.form.ProdutoForm;
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
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "false") boolean mostrarInativos,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        Pageable paginacao = PageRequest.of(pagina, 10, Sort.by("nome"));
        Boolean ativo = mostrarInativos ? null : Boolean.TRUE;
        Page<?> resultado = produtoService.listar(termo, ativo, paginacao);

        model.addAttribute("pagina", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("mostrarInativos", mostrarInativos);
        model.addAttribute("paginaAtiva", "produtos");
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produto", new ProdutoForm());
        model.addAttribute("edicao", false);
        model.addAttribute("paginaAtiva", "produtos");
        return "produtos/formulario";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("produto") ProdutoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("edicao", false);
            model.addAttribute("paginaAtiva", "produtos");
            return "produtos/formulario";
        }
        produtoService.cadastrar(form);
        atributos.addFlashAttribute("mensagemSucesso", "Material cadastrado com sucesso.");
        return "redirect:/produtos";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("produto", produtoService.carregarParaEdicao(id));
        model.addAttribute("produtoId", id);
        model.addAttribute("edicao", true);
        model.addAttribute("paginaAtiva", "produtos");
        return "produtos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("produto") ProdutoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("produtoId", id);
            model.addAttribute("edicao", true);
            model.addAttribute("paginaAtiva", "produtos");
            return "produtos/formulario";
        }
        produtoService.atualizar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Material atualizado com sucesso.");
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes atributos) {
        produtoService.inativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Material inativado.");
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Long id, RedirectAttributes atributos) {
        produtoService.reativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Material reativado.");
        return "redirect:/produtos?mostrarInativos=true";
    }
}
