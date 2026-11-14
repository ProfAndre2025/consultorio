package br.senai.consultorio.web.controller;

import br.senai.consultorio.domain.enums.StatusConsulta;
import br.senai.consultorio.dto.form.AgendamentoForm;
import br.senai.consultorio.dto.form.CancelamentoForm;
import br.senai.consultorio.dto.form.ReagendamentoForm;
import br.senai.consultorio.service.AgendaService;
import br.senai.consultorio.service.DentistaService;
import br.senai.consultorio.service.PacienteService;
import br.senai.consultorio.service.ProcedimentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Agenda: listagem, agendamento em dois passos e cancelamento.
 *
 * O agendamento acontece em dois passos porque o usuario precisa VER quem esta
 * livre antes de escolher:
 *   1. informa paciente, procedimento, data e hora -> o sistema devolve os
 *      dentistas disponiveis;
 *   2. escolhe um dentista e confirma.
 */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final AgendaService agendaService;
    private final PacienteService pacienteService;
    private final ProcedimentoService procedimentoService;
    private final DentistaService dentistaService;

    public AgendaController(AgendaService agendaService,
                            PacienteService pacienteService,
                            ProcedimentoService procedimentoService,
                            DentistaService dentistaService) {
        this.agendaService = agendaService;
        this.pacienteService = pacienteService;
        this.procedimentoService = procedimentoService;
        this.dentistaService = dentistaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                         @RequestParam(required = false) StatusConsulta status,
                         @RequestParam(required = false) String termo,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        Pageable paginacao = PageRequest.of(pagina, 15);
        Page<?> resultado = agendaService.listar(inicio, fim, status, termo, paginacao);

        model.addAttribute("pagina", resultado);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("status", status);
        model.addAttribute("termo", termo);
        model.addAttribute("statusPossiveis", StatusConsulta.values());
        model.addAttribute("paginaAtiva", "agenda");
        return "agenda/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        prepararTelaAgendamento(model, new AgendamentoForm());
        return "agenda/agendar";
    }

    /**
     * Passo 1: busca os dentistas disponiveis para o horario escolhido.
     * Reexibe o MESMO formulario, agora com a lista de profissionais.
     */
    @PostMapping("/disponiveis")
    public String buscarDisponiveis(@ModelAttribute("agendamento") AgendamentoForm form,
                                    Model model,
                                    RedirectAttributes atributos) {
        if (form.getPacienteId() == null || form.getProcedimentoId() == null
                || form.getData() == null || form.getHora() == null) {
            atributos.addFlashAttribute("mensagemErro",
                    "Preencha paciente, procedimento, data e horario antes de buscar.");
            return "redirect:/agenda/nova";
        }

        LocalDateTime dataHora = LocalDateTime.of(form.getData(), form.getHora());
        var disponiveis = agendaService.dentistasDisponiveis(form.getProcedimentoId(), dataHora);

        prepararTelaAgendamento(model, form);
        model.addAttribute("disponiveis", disponiveis);
        model.addAttribute("buscaRealizada", true);
        return "agenda/agendar";
    }

    /** Passo 2: confirma o agendamento com o dentista escolhido. */
    @PostMapping
    public String agendar(@Valid @ModelAttribute("agendamento") AgendamentoForm form,
                          BindingResult erros,
                          Model model,
                          RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            prepararTelaAgendamento(model, form);
            model.addAttribute("buscaRealizada", form.getDentistaId() != null);
            return "agenda/agendar";
        }
        agendaService.agendar(form);
        atributos.addFlashAttribute("mensagemSucesso", "Consulta agendada com sucesso.");
        return "redirect:/agenda";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        var consulta = agendaService.buscar(id);
        model.addAttribute("consulta", consulta);
        model.addAttribute("cancelamento", new CancelamentoForm());
        model.addAttribute("reagendamento", formularioDeRemarcacao(consulta));
        model.addAttribute("dentistas", dentistaService.listarAtivos());
        model.addAttribute("paginaAtiva", "agenda");
        return "agenda/detalhe";
    }

    /**
     * Remarca a consulta: novo dia, novo horario e/ou outro dentista.
     * O service e quem confere se o profissional atende naquela janela.
     */
    @PostMapping("/{id}/reagendar")
    public String reagendar(@PathVariable Long id,
                            @Valid @ModelAttribute("reagendamento") ReagendamentoForm form,
                            BindingResult erros,
                            Model model,
                            RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("consulta", agendaService.buscar(id));
            model.addAttribute("cancelamento", new CancelamentoForm());
            model.addAttribute("dentistas", dentistaService.listarAtivos());
            model.addAttribute("paginaAtiva", "agenda");
            return "agenda/detalhe";
        }
        agendaService.remarcar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Consulta remarcada.");
        return "redirect:/agenda/" + id;
    }

    /** Abre o formulario ja preenchido com o que a consulta tem hoje. */
    private ReagendamentoForm formularioDeRemarcacao(
            br.senai.consultorio.dto.view.ConsultaView consulta) {
        ReagendamentoForm form = new ReagendamentoForm();
        form.setDentistaId(consulta.dentistaId());
        form.setData(consulta.dataHora().toLocalDate());
        form.setHora(consulta.dataHora().toLocalTime());
        return form;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @Valid @ModelAttribute("cancelamento") CancelamentoForm form,
                           BindingResult erros,
                           Model model,
                           RedirectAttributes atributos) {
        if (erros.hasErrors()) {
            model.addAttribute("consulta", agendaService.buscar(id));
            model.addAttribute("paginaAtiva", "agenda");
            return "agenda/detalhe";
        }
        agendaService.cancelar(id, form);
        atributos.addFlashAttribute("mensagemSucesso", "Consulta cancelada.");
        return "redirect:/agenda/" + id;
    }

    /** Reune as listas que a tela de agendamento sempre precisa. */
    private void prepararTelaAgendamento(Model model, AgendamentoForm form) {
        model.addAttribute("agendamento", form);
        model.addAttribute("pacientes", pacienteService.listarTodos());
        model.addAttribute("procedimentos", procedimentoService.listarTodos());
        model.addAttribute("horaSugerida", LocalTime.of(9, 0));
        model.addAttribute("paginaAtiva", "agenda");
    }
}
