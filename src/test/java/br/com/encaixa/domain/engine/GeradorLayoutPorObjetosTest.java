package br.com.encaixa.domain.engine;

import br.com.encaixa.domain.engine.model.Modelos.ConfigEscala;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.domain.engine.model.Modelos.MedidasGaveta;
import br.com.encaixa.domain.engine.model.Modelos.TipoObjetoConstraint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorLayoutPorObjetosTest {

    private final ConfigEscala config = new ConfigEscala(6.0, 5.0);

    @Test
    void deveGerarLayoutComSucessoQuandoObjetosCabem() {
        UUID idRelogio = UUID.randomUUID();
        UUID idOculos = UUID.randomUUID();

        List<TipoObjetoConstraint> catalogo = List.of(
                new TipoObjetoConstraint(idRelogio, "Relógio", 80, 100, 100, 140, 50),
                new TipoObjetoConstraint(idOculos, "Óculos", 150, 180, 60, 80, 40)
        );

        List<ItemObjetoSelecionado> itens = List.of(
                new ItemObjetoSelecionado(idRelogio, 2),
                new ItemObjetoSelecionado(idOculos, 2)
        );

        MedidasGaveta gaveta = new MedidasGaveta(600, 450, 100);
        var gerador = new GeradorLayoutPorObjetos();

        ResultadoLayoutObjetos resultado = gerador.gerarDetalhado(gaveta, itens, catalogo, config, EstrategiaLayout.AUTO);

        assertNotNull(resultado);
        assertFalse(resultado.escala().compartimentos().isEmpty());
        // Deve preencher 4 compartimentos para os 4 itens
        assertTrue(resultado.objetos().size() == 4);

        // Deve identificar estratégias viáveis
        List<EstrategiaLayout> viaveis = gerador.estrategiasViaveis(gaveta, itens, catalogo, config);
        assertFalse(viaveis.isEmpty());
    }

    @Test
    void deveLancarLayoutNaoCabeExceptionComSugestoesQuandoNaoCouber() {
        UUID idGrande = UUID.randomUUID();
        List<TipoObjetoConstraint> catalogo = List.of(
                new TipoObjetoConstraint(idGrande, "Prato Gigante", 250, 300, 250, 300, 40)
        );

        // 10 pratos de 250x250 em uma gaveta de apenas 300x300
        List<ItemObjetoSelecionado> itens = List.of(
                new ItemObjetoSelecionado(idGrande, 10)
        );

        MedidasGaveta gavetaPequena = new MedidasGaveta(300, 300, 100);
        var gerador = new GeradorLayoutPorObjetos();

        LayoutNaoCabeException ex = assertThrows(LayoutNaoCabeException.class, () ->
                gerador.gerarDetalhado(gavetaPequena, itens, catalogo, config, EstrategiaLayout.AUTO)
        );

        assertNotNull(ex.getMessage());
        // Deve ter sugerido medidas maiores ou camadas
        assertTrue(ex.larguraMinimaSugeridaMm() != null || ex.profundidadeMinimaSugeridaMm() != null || !ex.camadas().isEmpty());
    }
}
