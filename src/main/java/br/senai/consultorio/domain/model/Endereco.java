package br.senai.consultorio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Endereco no padrao brasileiro, usado tanto pelo paciente quanto pelo
 * dentista.
 *
 * E um @Embeddable (objeto de valor): nao tem tabela nem id proprios. Suas
 * colunas ficam embutidas na tabela de quem o usa -- paciente e dentista --,
 * o que evita repetir os sete campos em cada entidade e mantem a regra de
 * endereco num lugar so. Todos os campos sao opcionais: um cadastro pode ser
 * salvo sem endereco completo e preenchido depois.
 */
@Embeddable
public class Endereco {

    @Column(length = 9)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 60)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

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
