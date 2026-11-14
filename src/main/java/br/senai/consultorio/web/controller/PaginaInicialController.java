package br.senai.consultorio.web.controller;

import br.senai.consultorio.repository.ConsultaRepository;
import br.senai.consultorio.repository.DentistaRepository;
import br.senai.consultorio.repository.PacienteRepository;
import br.senai.consultorio.repository.ProcedimentoRepository;
import br.senai.consultorio.service.AgendaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Tela inicial com os "cartoes" de acesso e alguns numeros da clinica.
 *
 * Depende direto dos repositorios (contagens simples) porque nao ha regra de
 * negocio envolvida -- criar um service so para contar registros seria
 * cerimonia sem ganho.
 */
@Controller
public class PaginaInicialController {

    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final ConsultaRepository consultaRepository;
    private final AgendaService agendaService;

    public PaginaInicialController(PacienteRepository pacienteRepository,
                                   DentistaRepository dentistaRepository,
                                   ProcedimentoRepository procedimentoRepository,
                                   ConsultaRepository consultaRepository,
                                   AgendaService agendaService) {
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.consultaRepository = consultaRepository;
        this.agendaService = agendaService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        // O dia de hoje vem primeiro: e o que a recepcao precisa ver ao abrir.
        model.addAttribute("consultasDeHoje", agendaService.agendaDeHoje());
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalDentistas", dentistaRepository.count());
        model.addAttribute("totalProcedimentos", procedimentoRepository.count());
        model.addAttribute("totalConsultas", consultaRepository.count());
        model.addAttribute("paginaAtiva", "inicio");
        return "index";
    }
}
