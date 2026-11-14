package br.senai.consultorio.domain.exception;

/**
 * Erro previsto pelas REGRAS DO NEGOCIO, e nao um defeito do programa.
 * Exemplos: agendar em horario ocupado, cancelar consulta ja realizada,
 * usar material sem estoque suficiente.
 *
 * Na Etapa 3 um @RestControllerAdvice traduz esta excecao em HTTP 422.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
