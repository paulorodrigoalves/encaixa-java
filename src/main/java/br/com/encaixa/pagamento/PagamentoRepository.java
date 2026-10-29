package br.com.encaixa.pagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
    List<Pagamento> findAllByCompraIdOrderByCriadoEmDesc(UUID compraId);

    Optional<Pagamento> findByProviderPaymentId(String providerPaymentId);
}
