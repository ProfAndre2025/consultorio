package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Objeto de ENTRADA do formulario de paciente.
 *
 * Por que nao usar a entidade direto no formulario?
 *   1. o navegador nao deve poder alterar campos como id, criadoEm ou versao;
 *   2. a validacao de tela e diferente da validacao do banco;
 *   3. se a entidade mudar, o formulario nao quebra junto.
 *
 * E uma classe mutavel, e nao um record, porque o Thymeleaf precisa de um
 * objeto vazio (new PacienteForm()) para montar a tela de cadastro e porque
 * o th:field le e escreve via getter/setter.
 */
public class PacienteForm {

    @NotBlank(message = "Informe o nome completo do paciente.")
    @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o CPF.")
    @Size(min = 11, max = 14, message = "CPF invalido.")
    private String cpf;

    @Past(message = "A data de nascimento deve estar no passado.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascimento;

    @Size(max = 20)
    private String telefone;

    @Email(message = "E-mail invalido.")
    @Size(max = 120)
    private String email;

    @Size(max = 2000)
    private String observacoes;

    // ----- Dados complementares e endereco (todos opcionais) -----
    @Size(max = 20, message = "Estado civil muito longo.")
    private String estadoCivil;

    @Size(max = 20, message = "Genero muito longo.")
    private String genero;

    @Size(max = 9, message = "CEP invalido.")
    private String cep;

    @Size(max = 150, message = "Logradouro muito longo.")
    private String logradouro;

    @Size(max = 10, message = "Numero muito longo.")
    private String numero;

    @Size(max = 60, message = "Complemento muito longo.")
    private String complemento;

    @Size(max = 80, message = "Bairro muito longo.")
    private String bairro;

    @Size(max = 80, message = "Cidade muito longa.")
    private String cidade;

    @Size(max = 2, message = "Use a sigla da UF (2 letras).")
    private String uf;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}
