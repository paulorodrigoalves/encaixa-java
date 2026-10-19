package br.com.encaixa.catalogo.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TipoMaterialRepository extends JpaRepository<TipoMaterial, UUID> {
    List<TipoMaterial> findAllByOrderByNomeAsc();
}
