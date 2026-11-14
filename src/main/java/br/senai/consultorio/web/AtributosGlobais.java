package br.senai.consultorio.web;

import br.senai.consultorio.domain.enums.OrigemCancelamento;
import br.senai.consultorio.mapper.DentistaMapper;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados que TODA tela precisa, injetados automaticamente no Model.
 *
 * Sem isto, cada controller teria que repetir "adicione a lista de dias da
 * semana ao model" antes de devolver a pagina. O @ModelAttribute aqui roda
 * para qualquer requisicao atendida por um @Controller.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class AtributosGlobais {

    /** Dias da semana ja traduzidos, para os <select> de disponibilidade. */
    @ModelAttribute("diasDaSemana")
    public Map<DayOfWeek, String> diasDaSemana() {
        Map<DayOfWeek, String> dias = new LinkedHashMap<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            dias.put(dia, DentistaMapper.descreverDia(dia));
        }
        return dias;
    }

    /** Opcoes de "quem cancelou", para o formulario de cancelamento. */
    @ModelAttribute("origensCancelamento")
    public OrigemCancelamento[] origensCancelamento() {
        return OrigemCancelamento.values();
    }
}
