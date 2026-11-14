package br.senai.consultorio.domain.enums;

/** Quem solicitou o cancelamento da consulta. */
public enum OrigemCancelamento {

    PACIENTE("Paciente"),
    ATENDENTE("Atendente"),
    DENTISTA("Dentista");

    private final String descricao;

    OrigemCancelamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
