package br.senai.consultorio.dto.form;

import jakarta.validation.constraints.Size;

public class FinalizacaoForm {

    @Size(max = 2000, message = "As observacoes clinicas sao muito longas.")
    private String observacoes;

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
