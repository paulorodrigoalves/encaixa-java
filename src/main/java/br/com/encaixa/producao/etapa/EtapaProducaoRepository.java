package br.com.encaixa.producao.etapa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EtapaProducaoRepository extends JpaRepository<EtapaProducao, UUID> {
    List<EtapaProducao> findAllByAtivoTrueOrderByOrdemAsc();
}
