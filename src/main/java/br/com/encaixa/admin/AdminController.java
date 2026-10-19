package br.com.encaixa.admin;

import br.com.encaixa.admin.AdminDtos.AtualizarRegraPrecoRequest;
import br.com.encaixa.admin.AdminDtos.CatalogoAdminDTO;
import br.com.encaixa.admin.AdminDtos.CompraAdminDTO;
import br.com.encaixa.admin.AdminDtos.CriarMaterialRequest;
import br.com.encaixa.admin.AdminDtos.CriarTipoMaterialRequest;
import br.com.encaixa.admin.AdminDtos.CriarTipoObjetoRequest;
import br.com.encaixa.admin.AdminDtos.EtapaProducaoDTO;
import br.com.encaixa.admin.AdminDtos.PlanoCorteProjetoDTO;
import br.com.encaixa.admin.AdminDtos.RegistrarEtapaRequest;
import br.com.encaixa.admin.AdminDtos.RegraPrecificacaoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/compras/{compraId}/plano-corte")
    public ResponseEntity<List<PlanoCorteProjetoDTO>> obterPlanoCorteCompra(@PathVariable UUID compraId) {
        return ResponseEntity.ok(adminService.gerarPlanoCorteCompra(compraId));
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

    @GetMapping("/catalogo")
    public ResponseEntity<CatalogoAdminDTO> obterCatalogoAdmin() {
        return ResponseEntity.ok(adminService.obterCatalogoAdmin());
    }

    @PostMapping("/catalogo/tipos-material")
    public ResponseEntity<Void> criarTipoMaterial(@RequestBody @Valid CriarTipoMaterialRequest req) {
        adminService.criarTipoMaterial(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/catalogo/materiais")
    public ResponseEntity<Void> criarMaterial(@RequestBody @Valid CriarMaterialRequest req) {
        adminService.criarMaterial(req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/catalogo/materiais/{id}/toggle-ativo")
    public ResponseEntity<Void> alternarAtivoMaterial(@PathVariable UUID id) {
        adminService.alternarAtivoMaterial(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/catalogo/tipos-objeto")
    public ResponseEntity<Void> criarTipoObjeto(@RequestBody @Valid CriarTipoObjetoRequest req) {
        adminService.criarTipoObjeto(req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/catalogo/tipos-objeto/{id}/toggle-ativo")
    public ResponseEntity<Void> alternarAtivoTipoObjeto(@PathVariable UUID id) {
        adminService.alternarAtivoTipoObjeto(id);
        return ResponseEntity.ok().build();
    }
}
