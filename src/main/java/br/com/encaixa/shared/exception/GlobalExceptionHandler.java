package br.com.encaixa.shared.exception;

import br.com.encaixa.domain.engine.LayoutInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz excecoes em respostas RFC 9457 (ProblemDetail). O campo {@code detail}
 * sempre traz uma mensagem pronta pra exibir ao usuario.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(NegocioException.class)
    ProblemDetail negocio(NegocioException ex) {
        return problem(ex.getStatus(), "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(LayoutInvalidoException.class)
    ProblemDetail layoutInvalido(LayoutInvalidoException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Layout inválido", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> erros.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> erros.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                erros.values().stream().findFirst().orElse("Dados inválidos."));
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail validacaoParametros(HandlerMethodValidationException ex) {
        String msg = ex.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst().orElse("Parâmetros inválidos.");
        return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", msg);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflito de dados",
                "Operação viola uma restrição de dados (registro duplicado ou referência inválida).");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;
    }
}
