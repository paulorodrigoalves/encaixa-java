package br.com.encaixa.shared.web;

import br.com.encaixa.domain.engine.LayoutInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.encaixa.domain.engine.LayoutNaoCabeException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({LayoutInvalidoException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", ex.getMessage()));
    }

    /** Objetos que nao cabem: devolve a mensagem + sugestoes (medida minima / duas camadas). */
    @ExceptionHandler(LayoutNaoCabeException.class)
    public ResponseEntity<Map<String, Object>> handleNaoCabe(LayoutNaoCabeException ex) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", ex.getMessage());
        corpo.put("larguraMinimaSugeridaMm", ex.larguraMinimaSugeridaMm());
        corpo.put("profundidadeMinimaSugeridaMm", ex.profundidadeMinimaSugeridaMm());
        corpo.put("camadas", ex.camadas());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }
}