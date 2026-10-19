package br.com.encaixa.venda.compra;

import br.com.encaixa.domain.engine.EstimadorPacote;
import br.com.encaixa.domain.engine.EstimadorPacote.ItemPacote;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.PacoteEstimado;
import br.com.encaixa.venda.cliente.Cliente;
import br.com.encaixa.venda.compra.dto.CheckoutDtos.CotacaoFreteResponse;
import br.com.encaixa.venda.compra.dto.CheckoutDtos.FinalizarCheckoutRequest;
import br.com.encaixa.venda.compra.dto.CheckoutDtos.OpcaoFreteDTO;
import br.com.encaixa.venda.projeto.Projeto;
import br.com.encaixa.venda.projeto.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CompraRepository compraRepository;
    private final ProjetoRepository projetoRepository;

    @Transactional(readOnly = true)
    public CotacaoFreteResponse cotarFreteCompra(UUID compraId, String cepDestino) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada."));

        List<Projeto> projetos = projetoRepository.findAllByCompraIdWithCompartimentos(compraId);
        if (projetos.isEmpty()) {
            throw new IllegalArgumentException("Esta compra não possui projetos para calcular o frete.");
        }

        List<ItemPacote> itensPacote = projetos.stream()
                .map(p -> new ItemPacote(
                        new MedidasGaveta(p.getLarguraMm(), p.getProfundidadeMm(), p.getAlturaMm()),
                        p.getQuantidade()
                ))
                .toList();

        PacoteEstimado pacote = new EstimadorPacote().estimarCompra(itensPacote);

        // Calcula opções de frete proporcionais ao peso (kg) e região do CEP
        String cepLimpo = cepDestino == null ? "" : cepDestino.replaceAll("\\D", "");
        double multiplicadorRegiao = cepLimpo.startsWith("0") || cepLimpo.startsWith("1") || cepLimpo.startsWith("2") || cepLimpo.startsWith("3")
                ? 1.0
                : 1.35;

        BigDecimal valorPac = BigDecimal.valueOf((24.90 + pacote.pesoKg() * 6.50) * multiplicadorRegiao)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorSedex = BigDecimal.valueOf((39.90 + pacote.pesoKg() * 11.20) * multiplicadorRegiao)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorTransportadora = BigDecimal.valueOf((32.00 + pacote.pesoKg() * 7.80) * multiplicadorRegiao)
                .setScale(2, RoundingMode.HALF_UP);

        List<OpcaoFreteDTO> opcoes = List.of(
                new OpcaoFreteDTO("Correios PAC", valorPac, multiplicadorRegiao == 1.0 ? 7 : 11),
                new OpcaoFreteDTO("Jadlog Package", valorTransportadora, multiplicadorRegiao == 1.0 ? 5 : 8),
                new OpcaoFreteDTO("Correios SEDEX", valorSedex, multiplicadorRegiao == 1.0 ? 2 : 4)
        );

        BigDecimal subtotalProjetos = compra.getValorTotal().subtract(compra.getValorFrete());
        Cliente cli = compra.getCliente();

        return new CotacaoFreteResponse(
                compra.getId(),
                projetos.size(),
                subtotalProjetos,
                pacote,
                opcoes,
                cli.getTelefone(),
                cli.getEndereco()
        );
    }

    @Transactional
    public void finalizarCheckoutCompra(UUID compraId, FinalizarCheckoutRequest req) {
        Compra compra = compraRepository.findById(compraId)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada."));

        Cliente cliente = compra.getCliente();
        cliente.setTelefone(req.telefone().trim());
        cliente.setEndereco(req.endereco());

        BigDecimal subtotalProjetos = compra.getValorTotal().subtract(compra.getValorFrete());
        compra.setTransportadora(req.transportadora());
        compra.setValorFrete(req.valorFrete());
        compra.setPrazoFreteDias(req.prazoFreteDias());
        compra.setValorTotal(subtotalProjetos.add(req.valorFrete()));

        List<Projeto> projetos = projetoRepository.findAllByCompraIdWithCompartimentos(compraId);
        for (Projeto p : projetos) {
            p.setStatus(Projeto.StatusProjeto.FECHADO);
        }

        compraRepository.save(compra);
    }
}
