package br.senai.consultorio.domain.exception;

/** Lancada quando um id informado nao existe no banco. Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String entidade, Object id) {
        return new RecursoNaoEncontradoException(
                "%s de id %s nao foi encontrado(a).".formatted(entidade, id));
    }
}
