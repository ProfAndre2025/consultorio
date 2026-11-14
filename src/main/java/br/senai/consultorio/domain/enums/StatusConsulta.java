package br.senai.consultorio.domain.enums;

/** Situacao em que uma consulta pode estar. */
public enum StatusConsulta {

    AGENDADA("Agendada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusConsulta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
