package br.com.encaixa.admin;

import br.com.encaixa.admin.AdminDtos.AtualizarRegraPrecoRequest;
import br.com.encaixa.admin.AdminDtos.CompraAdminDTO;
import br.com.encaixa.admin.AdminDtos.EtapaProducaoDTO;
import br.com.encaixa.admin.AdminDtos.RegistrarEtapaRequest;
import br.com.encaixa.admin.AdminDtos.RegraPrecificacaoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/precificacao")
    public ResponseEntity<RegraPrecificacaoDTO> obterRegraVigente() {
        return ResponseEntity.ok(adminService.obterRegraVigente());
    }

    @PostMapping("/precificacao")
    public ResponseEntity<RegraPrecificacaoDTO> atualizarRegraPreco(
            @RequestBody @Valid AtualizarRegraPrecoRequest req,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(adminService.atualizarRegraPreco(req, jwt));
    }

    @GetMapping("/etapas-producao")
    public ResponseEntity<List<EtapaProducaoDTO>> listarEtapasCatalogo() {
        return ResponseEntity.ok(adminService.listarEtapasCatalogo());
    }

    @GetMapping("/compras")
    public ResponseEntity<List<CompraAdminDTO>> listarComprasEPedidos() {
        return ResponseEntity.ok(adminService.listarComprasEPedidos());
    }

    @PostMapping("/compras/{compraId}/aprovar")
    public ResponseEntity<Void> aprovarCompra(@PathVariable UUID compraId) {
        adminService.aprovarCompraGerarPedido(compraId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pedidos/{pedidoId}/etapas")
    public ResponseEntity<Void> registrarEtapa(
            @PathVariable UUID pedidoId,
            @RequestBody @Valid RegistrarEtapaRequest req
    ) {
        adminService.registrarEtapaPedido(pedidoId, req);
        return ResponseEntity.ok().build();
    }
}
