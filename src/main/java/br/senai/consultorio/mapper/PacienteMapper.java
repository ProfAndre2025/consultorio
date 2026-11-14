package br.senai.consultorio.mapper;

import br.senai.consultorio.domain.model.Endereco;
import br.senai.consultorio.domain.model.Paciente;
import br.senai.consultorio.dto.form.Formatador;
import br.senai.consultorio.dto.form.PacienteForm;
import br.senai.consultorio.dto.view.PacienteView;
import org.springframework.stereotype.Component;

/**
 * Traducao entre entidade, formulario e view.
 *
 * Mantendo a conversao isolada aqui, o service nao precisa saber como a tela
 * apresenta os dados, e a tela nao precisa saber como a entidade e montada.
 * Se um dia trocarmos por MapStruct, muda so esta classe.
 */
@Component
public class PacienteMapper {

    public PacienteView paraView(Paciente paciente) {
        return new PacienteView(
                paciente.getId(),
                paciente.getNome(),
                paciente.getCpf(),
                paciente.getDataNascimento(),
                paciente.getTelefone(),
                paciente.getEmail(),
                paciente.getObservacoes(),
                paciente.isAtivo());
    }

    /** Preenche o formulario com os dados atuais, para a tela de edicao. */
    public PacienteForm paraForm(Paciente paciente) {
        PacienteForm form = new PacienteForm();
        form.setNome(paciente.getNome());
        form.setCpf(paciente.getCpf());
        form.setDataNascimento(paciente.getDataNascimento());
        form.setTelefone(paciente.getTelefone());
        form.setEmail(paciente.getEmail());
        form.setObservacoes(paciente.getObservacoes());
        form.setEstadoCivil(paciente.getEstadoCivil());
        form.setGenero(paciente.getGenero());
        // Le o endereco embutido de volta para os campos planos do formulario.
        Endereco endereco = paciente.getEndereco();
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

    public Paciente paraEntidade(PacienteForm form) {
        Paciente paciente = new Paciente(form.getNome().trim(), Formatador.cpf(form.getCpf()));
        copiarCamposEditaveis(form, paciente);
        return paciente;
    }

    /** Usado na edicao: nunca cria objeto novo, apenas altera o que veio do banco. */
    public void copiarParaEntidade(PacienteForm form, Paciente paciente) {
        paciente.setNome(form.getNome().trim());
        paciente.setCpf(Formatador.cpf(form.getCpf()));
        copiarCamposEditaveis(form, paciente);
    }

    private void copiarCamposEditaveis(PacienteForm form, Paciente paciente) {
        paciente.setDataNascimento(form.getDataNascimento());
        paciente.setTelefone(Formatador.telefone(form.getTelefone()));
        paciente.setEmail(form.getEmail());
        paciente.setObservacoes(form.getObservacoes());
        paciente.setEstadoCivil(form.getEstadoCivil());
        paciente.setGenero(form.getGenero());
        paciente.setEndereco(montarEndereco(form));
    }

    /** Monta o objeto de valor Endereco a partir dos campos planos do form. */
    private Endereco montarEndereco(PacienteForm form) {
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
