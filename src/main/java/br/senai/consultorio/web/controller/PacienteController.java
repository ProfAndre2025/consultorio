package br.senai.consultorio.web.controller;

import br.senai.consultorio.dto.form.PacienteForm;
import br.senai.consultorio.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * CRUD de pacientes.
 *
 * Padrao seguido em todos os controllers de cadastro:
 *   GET  /pacientes            -> lista (com busca e paginacao)
 *   GET  /pacientes/novo       -> formulario vazio
 *   POST /pacientes            -> grava novo
 *   GET  /pacientes/{id}/editar-> formulario preenchido
 *   POST /pacientes/{id}       -> atualiza
 *   POST /pacientes/{id}/excluir-> remove
 *
 * Depois de gravar, o controller sempre REDIRECIONA (padrao POST-Redirect-GET).
 * Isso evita que apertar F5 reenvie o formulario e duplique o cadastro.
 */
@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final br.senai.consultorio.service.AtendimentoService atendimentoService;

    public PacienteController(PacienteService pacienteService,
                              br.senai.consultorio.service.AtendimentoService atendimentoService) {
        this.pacienteService = pacienteService;
        this.atendimentoService = atendimentoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "false") boolean mostrarInativos,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        Pageable paginacao = PageRequest.of(pagina, 10, Sort.by("nome"));
        // mostrarInativos=false -> so ativos (true); true -> todos (null).
        Boolean ativo = mostrarInativos ? null : Boolean.TRUE;
        Page<?> resultado = pacienteService.listar(termo, ativo, paginacao);

        model.addAttribute("pagina", resultado);
        model.addAttribute("termo", termo);
        model.addAttribute("mostrarInativos", mostrarInativos);
        model.addAttribute("paginaAtiva", "pacientes");
        return "pacientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("paciente", new PacienteForm());
        model.addAttribute("edicao", false);
        model.addAttribute("paginaAtiva", "pacientes");
        return "pacientes/formulario";
    }

    @PostMapping
    public String cadastrar(@Valid @org.springframework.web.bind.annotation.ModelAttribute("paciente") PacienteForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("edicao", false);
            model.addAttribute("paginaAtiva", "pacientes");
            return "pacientes/formulario";
        }
        pacienteService.cadastrar(form);
        atributos.addFlashAttribute("mensagemSucesso", "Paciente cadastrado com sucesso.");
        return "redirect:/pacientes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", pacienteService.carregarParaEdicao(id));
        model.addAttribute("pacienteId", id);
        model.addAttribute("edicao", true);
        model.addAttribute("paginaAtiva", "pacientes");
        return "pacientes/formulario";
    }

    /**
     * Historico clinico do paciente: o que ja foi realizado, para o dentista
     * consultar quando ele retorna. So atendimentos finalizados, sem valores.
     */
    @GetMapping("/{id}/historico")
    public String historico(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", pacienteService.buscar(id));
        model.addAttribute("historico", atendimentoService.historicoDoPaciente(id));
        model.addAttribute("paginaAtiva", "pacientes");
        return "pacientes/historico";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @org.springframework.web.bind.annotation.ModelAttribute("paciente") PacienteForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("pacienteId", id);
            model.addAttribute("edicao", true);
            model.addAttribute("paginaAtiva", "pacientes");
            return "pacientes/formulario";
        }
        pacienteService.atualizar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Paciente atualizado com sucesso.");
        return "redirect:/pacientes";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes atributos) {
        pacienteService.inativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Paciente inativado.");
        return "redirect:/pacientes";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Long id, RedirectAttributes atributos) {
        pacienteService.reativar(id);
        atributos.addFlashAttribute("mensagemSucesso", "Paciente reativado.");
        return "redirect:/pacientes?mostrarInativos=true";
    }
}
