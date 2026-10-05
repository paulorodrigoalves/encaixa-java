package br.com.encaixa.venda.layout;

import br.com.encaixa.venda.layout.dto.PreviewLayoutRequest;
import br.com.encaixa.venda.layout.dto.PreviewLayoutResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/layout")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Liberar acesso do Angular
public class LayoutController {

    private final LayoutService layoutService;

    @PostMapping("/preview")
    public ResponseEntity<PreviewLayoutResponse> gerarPreview(@RequestBody @Valid PreviewLayoutRequest req) {
        return ResponseEntity.ok(layoutService.gerarPreview(req));
    }
}
