package br.senai.consultorio.web;

import br.senai.consultorio.domain.exception.RecursoNaoEncontradoException;
import br.senai.consultorio.domain.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Captura, em UM lugar so, as excecoes que escapam dos controllers.
 *
 * Este e o oposto do "except Exception: pass" do projeto Python: la o erro era
 * engolido e o usuario nao sabia o que tinha acontecido. Aqui cada tipo de erro
 * vira uma resposta clara.
 *
 * A estrategia depende da natureza do erro:
 *   - erro de REGRA (ex.: horario ocupado) -> volta para a pagina anterior com
 *     uma mensagem vermelha, preservando o que o usuario ja tinha digitado;
 *   - recurso INEXISTENTE -> pagina 404;
 *   - qualquer outra coisa -> pagina 500 e log completo para o desenvolvedor.
 */
@ControllerAdvice
public class ControleGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(ControleGlobalDeErros.class);

    /** Regra de negocio: o usuario errou algo previsto. Volta e explica. */
    @ExceptionHandler(RegraDeNegocioException.class)
    public RedirectView tratarRegraDeNegocio(RegraDeNegocioException excecao,
                                             HttpServletRequest requisicao,
                                             RedirectAttributes atributos) {
        atributos.addFlashAttribute("mensagemErro", excecao.getMessage());
        return new RedirectView(origem(requisicao));
    }

    /** Conflito de edicao simultanea (o @Version pegou). Mensagem amigavel. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public RedirectView tratarConflito(HttpServletRequest requisicao,
                                       RedirectAttributes atributos) {
        atributos.addFlashAttribute("mensagemErro",
                "Este registro foi alterado por outra pessoa enquanto voce editava. "
                        + "Abra novamente e refaca a operacao.");
        return new RedirectView(origem(requisicao));
    }

    /** Id inexistente: mostra a pagina 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public String tratarNaoEncontrado(RecursoNaoEncontradoException excecao,
                                       org.springframework.ui.Model model) {
        model.addAttribute("mensagem", excecao.getMessage());
        return "erro/404";
    }

    /**
     * Recurso estatico inexistente (o caso classico e o /favicon.ico que o
     * navegador pede sozinho).
     *
     * Sem este tratador, a excecao caia na "rede de seguranca" abaixo e era
     * registrada como ERRO GRAVE, com stack trace, poluindo o log a cada
     * acesso -- e ainda devolvia a pagina 500 para um simples icone ausente.
     * Aqui devolvemos 404 sem alarde.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void tratarRecursoEstaticoAusente(NoResourceFoundException excecao) {
        log.debug("Recurso estatico nao encontrado: {}", excecao.getResourcePath());
    }

    /**
     * Rede de seguranca. Qualquer excecao nao prevista cai aqui: o usuario ve
     * uma pagina neutra e o desenvolvedor recebe o stack trace no log.
     */
    @ExceptionHandler(Exception.class)
    public String tratarInesperado(Exception excecao, org.springframework.ui.Model model) {
        log.error("Erro inesperado no processamento da requisicao", excecao);
        model.addAttribute("mensagem",
                "Ocorreu um erro inesperado. A equipe tecnica foi notificada.");
        return "erro/500";
    }

    /**
     * Descobre para onde voltar. Usa o cabecalho Referer (a pagina de origem);
     * na falta dele, cai na tela inicial.
     */
    private String origem(HttpServletRequest requisicao) {
        String referer = requisicao.getHeader("Referer");
        return referer != null ? referer : "/";
    }
}
