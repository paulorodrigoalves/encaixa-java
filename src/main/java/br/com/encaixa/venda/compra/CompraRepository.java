package br.com.encaixa.venda.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {
    List<Compra> findAllByClienteIdOrderByCriadoEmDesc(UUID clienteId);

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.cliente
            LEFT JOIN FETCH c.projetos
            ORDER BY c.criadoEm DESC
            """)
    List<Compra> findAllWithClienteAndProjetos();
}
