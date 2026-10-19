package br.com.encaixa.producao.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    Optional<Pedido> findByCompraId(UUID compraId);

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.compra c
            JOIN FETCH c.cliente
            LEFT JOIN FETCH p.etapas
            ORDER BY p.criadoEm DESC
            """)
    List<Pedido> findAllWithDetails();

    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.compra c
            LEFT JOIN FETCH p.etapas
            WHERE c.cliente.id = :clienteId
            """)
    List<Pedido> findAllByClienteIdWithEtapas(@Param("clienteId") UUID clienteId);
}
