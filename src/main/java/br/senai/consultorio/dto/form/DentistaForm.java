package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DentistaForm {

    @NotBlank(message = "Informe o nome do dentista.")
    @Size(min = 3, max = 120)
    private String nome;

    @NotBlank(message = "Informe o numero do CRO.")
    @Size(min = 3, max = 20)
    private String cro;

    @Size(max = 80)
    private String especialidade;

    @Size(max = 20)
    private String telefone;

    @Email(message = "E-mail invalido.")
    @Size(max = 120)
    private String email;

    private boolean ativo = true;

    /**
     * Senha para iniciar atendimento. Opcional no formulario:
     *   - no cadastro, vazio = dentista ainda sem senha (define depois);
     *   - na edicao, vazio = manter a senha atual (nao troca).
     * So e criptografada e gravada quando vem preenchida.
     */
    private String senha;

    // ----- Endereco (todos opcionais) -----
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

    public String getCro() {
        return cro;
    }

    public void setCro(String cro) {
        this.cro = cro;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
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
