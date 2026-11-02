-- =============================================================================
-- V4__seed_fotos_materiais.sql
-- Fotos ilustrativas iniciais de organizadores para cada tecido/revestimento
-- =============================================================================

INSERT INTO material_fotos (material_id, foto_url, legenda, ordem)
SELECT m.id, f.foto_url, f.legenda, f.ordem
FROM (VALUES
    ('Veludo Cinza',     'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=600&q=80', 'Organizador de gaveta revestido em veludo cinza chumbo', 1),
    ('Veludo Azul',      'https://images.unsplash.com/photo-1590736969955-71cc94801759?auto=format&fit=crop&w=600&q=80', 'Organizador de joias com acabamento em veludo azul', 1),
    ('Veludo Rosa',      'https://images.unsplash.com/photo-1535295972055-1c762f4483e5?auto=format&fit=crop&w=600&q=80', 'Organizador sob medida revestido em veludo rosa suave', 1),
    ('Linho Natural',    'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=600&q=80', 'Organizador em madeira com forração em linho natural rústico', 1),
    ('Sintetico Branco', 'https://images.unsplash.com/photo-1582533561751-ef6f6ab93a2e?auto=format&fit=crop&w=600&q=80', 'Organizador clean com acabamento lavável branco', 1)
) AS f(nome, foto_url, legenda, ordem)
JOIN materiais m ON m.nome = f.nome;
