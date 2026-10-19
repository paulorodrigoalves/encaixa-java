package br.com.encaixa.venda.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {
    List<Compra> findAllByClienteIdOrderByCriadoEmDesc(UUID clienteId);
}
