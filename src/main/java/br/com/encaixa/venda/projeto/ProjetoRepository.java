package br.com.encaixa.venda.projeto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    @Query("""
            SELECT p FROM Projeto p
            JOIN FETCH p.material m
            JOIN FETCH m.tipoMaterial
            LEFT JOIN FETCH p.template
            JOIN FETCH p.compra c
            WHERE c.cliente.id = :clienteId
            ORDER BY p.criadoEm DESC
            """)
    List<Projeto> findAllByClienteIdWithDetails(@Param("clienteId") UUID clienteId);
}
