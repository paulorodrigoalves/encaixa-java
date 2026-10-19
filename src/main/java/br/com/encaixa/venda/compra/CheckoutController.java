package br.com.encaixa.venda.compra;

import br.com.encaixa.venda.compra.dto.CheckoutDtos.CotacaoFreteResponse;
import br.com.encaixa.venda.compra.dto.CheckoutDtos.FinalizarCheckoutRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping("/{compraId}/cotacao-frete")
    public ResponseEntity<CotacaoFreteResponse> cotarFrete(
            @PathVariable UUID compraId,
            @RequestParam(required = false, defaultValue = "01001000") String cep
    ) {
        return ResponseEntity.ok(checkoutService.cotarFreteCompra(compraId, cep));
    }

    @PostMapping("/{compraId}/checkout")
    public ResponseEntity<Void> finalizarCheckout(
            @PathVariable UUID compraId,
            @RequestBody @Valid FinalizarCheckoutRequest req
    ) {
        checkoutService.finalizarCheckoutCompra(compraId, req);
        return ResponseEntity.ok().build();
    }
}
