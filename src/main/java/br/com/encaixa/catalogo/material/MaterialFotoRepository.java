package br.com.encaixa.catalogo.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialFotoRepository extends JpaRepository<MaterialFoto, UUID> {
    List<MaterialFoto> findAllByMaterialIdOrderByOrdemAsc(UUID materialId);
    Optional<MaterialFoto> findFirstByMaterialIdOrderByOrdemAsc(UUID materialId);
    void deleteAllByMaterialId(UUID materialId);
}
