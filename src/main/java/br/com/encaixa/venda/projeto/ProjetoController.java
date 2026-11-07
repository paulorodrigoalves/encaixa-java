package br.com.encaixa.venda.projeto;

import br.com.encaixa.venda.projeto.dto.CriarProjetoRequest;
import br.com.encaixa.venda.projeto.dto.ProjetoSalvoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    @PostMapping
    public ResponseEntity<ProjetoSalvoResponse> criarProjeto(
            @RequestBody @Valid CriarProjetoRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjetoSalvoResponse salvo = projetoService.salvarProjeto(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<ProjetoSalvoResponse>> listarMeusProjetos(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(projetoService.listarMeusProjetos(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerProjeto(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projetoService.removerProjeto(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
