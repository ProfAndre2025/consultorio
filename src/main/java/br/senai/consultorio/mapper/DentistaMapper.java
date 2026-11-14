package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Dentista;
import br.senai.consultorio.domain.model.Endereco;
import br.senai.consultorio.domain.model.Disponibilidade;
import br.senai.consultorio.dto.form.Formatador;
import br.senai.consultorio.dto.form.DentistaForm;
import br.senai.consultorio.dto.view.DentistaDisponivelView;
import br.senai.consultorio.dto.view.DentistaView;
import br.senai.consultorio.dto.view.DisponibilidadeView;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Component
public class DentistaMapper {

    private static final Locale BRASIL = Locale.of("pt", "BR");

    public DentistaView paraView(Dentista dentista) {
        List<DisponibilidadeView> faixas = dentista.getDisponibilidades().stream()
                .sorted((a, b) -> {
                    int porDia = a.getDiaSemana().compareTo(b.getDiaSemana());
                    return porDia != 0 ? porDia : a.getHoraInicio().compareTo(b.getHoraInicio());
                })
                .map(this::paraView)
                .toList();

        return new DentistaView(
                dentista.getId(),
                dentista.getNome(),
                dentista.getCro(),
                dentista.getEspecialidade(),
                dentista.getTelefone(),
                dentista.getEmail(),
                dentista.isAtivo(),
                faixas);
    }

    /** Versao leve, usada quando a tela nao precisa das faixas de horario. */
    public DentistaView paraViewResumida(Dentista dentista) {
        return new DentistaView(
                dentista.getId(),
                dentista.getNome(),
                dentista.getCro(),
                dentista.getEspecialidade(),
                dentista.getTelefone(),
                dentista.getEmail(),
                dentista.isAtivo(),
                List.of());
    }

    public DentistaDisponivelView paraDisponivelView(Dentista dentista) {
        return new DentistaDisponivelView(
                dentista.getId(), dentista.getNome(), dentista.getCro());
    }

    public DisponibilidadeView paraView(Disponibilidade disponibilidade) {
        return new DisponibilidadeView(
                disponibilidade.getId(),
                disponibilidade.getDiaSemana(),
                descreverDia(disponibilidade.getDiaSemana()),
                disponibilidade.getHoraInicio(),
                disponibilidade.getHoraFim());
    }

    /** MONDAY -> "Segunda-feira". Traducao vem do proprio java.time. */
    public static String descreverDia(DayOfWeek dia) {
        String texto = dia.getDisplayName(TextStyle.FULL, BRASIL);
        return texto.substring(0, 1).toUpperCase(BRASIL) + texto.substring(1);
    }

    public DentistaForm paraForm(Dentista dentista) {
        DentistaForm form = new DentistaForm();
        form.setNome(dentista.getNome());
        form.setCro(dentista.getCro());
        form.setEspecialidade(dentista.getEspecialidade());
        form.setTelefone(dentista.getTelefone());
        form.setEmail(dentista.getEmail());
        form.setAtivo(dentista.isAtivo());
        Endereco endereco = dentista.getEndereco();
        if (endereco != null) {
            form.setCep(endereco.getCep());
            form.setLogradouro(endereco.getLogradouro());
            form.setNumero(endereco.getNumero());
            form.setComplemento(endereco.getComplemento());
            form.setBairro(endereco.getBairro());
            form.setCidade(endereco.getCidade());
            form.setUf(endereco.getUf());
        }
        return form;
    }

    public Dentista paraEntidade(DentistaForm form) {
        Dentista dentista = new Dentista(form.getNome().trim(), Formatador.cro(form.getCro()));
        copiarCamposEditaveis(form, dentista);
        return dentista;
    }

    public void copiarParaEntidade(DentistaForm form, Dentista dentista) {
        dentista.setNome(form.getNome().trim());
        dentista.setCro(Formatador.cro(form.getCro()));
        copiarCamposEditaveis(form, dentista);
    }

    private void copiarCamposEditaveis(DentistaForm form, Dentista dentista) {
        dentista.setEspecialidade(form.getEspecialidade());
        dentista.setTelefone(Formatador.telefone(form.getTelefone()));
        dentista.setEmail(form.getEmail());
        dentista.setAtivo(form.isAtivo());
        dentista.setEndereco(montarEndereco(form));
    }

    /** Monta o objeto de valor Endereco a partir dos campos planos do form. */
    private Endereco montarEndereco(DentistaForm form) {
        Endereco endereco = new Endereco();
        endereco.setCep(Formatador.cep(form.getCep()));
        endereco.setLogradouro(form.getLogradouro());
        endereco.setNumero(form.getNumero());
        endereco.setComplemento(form.getComplemento());
        endereco.setBairro(form.getBairro());
        endereco.setCidade(form.getCidade());
        endereco.setUf(form.getUf());
        return endereco;
    }
}
