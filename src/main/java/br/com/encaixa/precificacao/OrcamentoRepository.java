package br.com.encaixa.precificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {
    Optional<Orcamento> findFirstByProjetoIdOrderByCriadoEmDesc(UUID projetoId);
}
