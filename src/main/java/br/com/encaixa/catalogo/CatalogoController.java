package br.com.encaixa.catalogo;

import br.com.encaixa.catalogo.dto.CatalogoCompletoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/catalogo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Para liberar acesso do Angular rodando no localhost:4200
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public ResponseEntity<CatalogoCompletoDTO> obterCatalogo() {
        return ResponseEntity.ok(catalogoService.obterCatalogoCompleto());
    }
}
