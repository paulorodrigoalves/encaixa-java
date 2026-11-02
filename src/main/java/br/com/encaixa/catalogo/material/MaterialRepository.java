package br.com.encaixa.catalogo.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    
    @Query("SELECT DISTINCT m FROM Material m JOIN FETCH m.tipoMaterial LEFT JOIN FETCH m.fotos WHERE m.ativo = true ORDER BY m.nome ASC")
    List<Material> findAllAtivos();

    @Query("SELECT DISTINCT m FROM Material m JOIN FETCH m.tipoMaterial LEFT JOIN FETCH m.fotos ORDER BY m.nome ASC")
    List<Material> findAllWithTipoMaterial();
}
