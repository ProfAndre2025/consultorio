package br.senai.consultorio.dto.view;

/** Resultado da busca por dentistas livres em um horario. */
public record DentistaDisponivelView(Long id, String nome, String cro) {

    public String rotulo() {
        return "%s - %s".formatted(nome, cro);
    }
}
