package br.com.encaixa.precificacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegraPrecificacaoRepository extends JpaRepository<RegraPrecificacao, UUID> {
    @Query("SELECT r FROM RegraPrecificacao r WHERE r.vigenteAte IS NULL")
    Optional<RegraPrecificacao> findVigente();
}
