package br.com.encaixa;

import br.com.encaixa.catalogo.material.Material;
import br.com.encaixa.catalogo.template.Template;
import br.com.encaixa.domain.engine.model.Modelos.ItemObjetoSelecionado;
import br.com.encaixa.precificacao.Orcamento;
import br.com.encaixa.precificacao.RegraPrecificacao;
import br.com.encaixa.producao.etapa.EtapaProducao;
import br.com.encaixa.producao.etapa.PedidoEtapa;
import br.com.encaixa.producao.pedido.Pedido;
import br.com.encaixa.venda.cliente.Cliente;
import br.com.encaixa.venda.cliente.Endereco;
import br.com.encaixa.venda.compartimento.Compartimento;
import br.com.encaixa.venda.compra.Compra;
import br.com.encaixa.venda.projeto.Projeto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe um Postgres real, aplica as migrations do Flyway e deixa o Hibernate validar
 * (ddl-auto=validate) que as entidades batem com o schema. Em seguida grava e relê
 * o grafo completo (cliente -> compra -> projeto -> compartimentos/orcamento -> pedido).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class PersistenciaSchemaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    EntityManager em;

    @Test
    void seedDoCatalogoCarregaPelasEntidades() {
        List<Material> materiais = em.createQuery("from Material m join fetch m.tipoMaterial", Material.class)
                .getResultList();
        List<Template> templates = em.createQuery("from Template", Template.class).getResultList();

        assertThat(materiais).hasSize(5);
        assertThat(materiais).allSatisfy(m -> assertThat(m.getTipoMaterial().getEspessuraMm()).isPositive());
        assertThat(templates).hasSize(4);
        assertThat(templates).allSatisfy(t -> {
            assertThat(t.getLayoutProporcional().colunas()).isNotEmpty();
            assertThat(t.getTagsObjetos()).isNotEmpty();
        });
    }

    @Test
    void gravaERelePedidoCompleto() {
        Material material = em.createQuery("from Material", Material.class).setMaxResults(1).getSingleResult();
        RegraPrecificacao regra = em.createQuery("from RegraPrecificacao r where r.vigenteAte is null",
                RegraPrecificacao.class).getSingleResult();
        EtapaProducao primeiraEtapa = em.createQuery("from EtapaProducao e order by e.ordem",
                EtapaProducao.class).setMaxResults(1).getSingleResult();
        UUID tipoObjetoId = em.createQuery("select t.id from TipoObjeto t", UUID.class)
                .setMaxResults(1).getSingleResult();

        Cliente cliente = new Cliente();
        cliente.setNome("Maria");
        cliente.setTelefone("11999990000");
        cliente.setEndereco(new Endereco("01001000", "Praca da Se", "1", null, "Se", "Sao Paulo", "SP"));
        em.persist(cliente);

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setValorTotal(new BigDecimal("250.00"));
        em.persist(compra);

        Projeto projeto = new Projeto();
        projeto.setCompra(compra);
        projeto.setMaterial(material);
        projeto.setItensObjeto(List.of(new ItemObjetoSelecionado(tipoObjetoId, 3)));
        projeto.setLarguraMm(400);
        projeto.setProfundidadeMm(450);
        projeto.setAlturaMm(80);
        projeto.setMargemSegurancaMm(5);
        projeto.setEspessuraMaterialMm(6);

        Compartimento c = new Compartimento();
        c.setPosXMm(6);
        c.setPosYMm(6);
        c.setLarguraMm(120);
        c.setProfundidadeMm(200);
        c.setAreaPaineisCm2(new BigDecimal("123.45"));
        projeto.substituirCompartimentos(List.of(c));
        em.persist(projeto);

        Orcamento orcamento = new Orcamento();
        orcamento.setProjeto(projeto);
        orcamento.setRegraPrecificacao(regra);
        orcamento.setVolumeTotalLitros(new BigDecimal("14.400"));
        orcamento.setAreaDivisoriasCm2(new BigDecimal("900.00"));
        orcamento.setValorVolume(new BigDecimal("259.20"));
        orcamento.setValorDivisorias(new BigDecimal("315.00"));
        orcamento.setValorMaterial(new BigDecimal("40.00"));
        orcamento.setValorTotal(new BigDecimal("614.20"));
        em.persist(orcamento);

        Pedido pedido = new Pedido();
        pedido.setCompra(compra);
        PedidoEtapa etapa = new PedidoEtapa();
        etapa.setEtapa(primeiraEtapa);
        etapa.setNomeEtapa(primeiraEtapa.getNome());
        pedido.registrarEtapa(etapa);
        em.persist(pedido);

        em.flush();
        em.clear();

        Projeto relido = em.find(Projeto.class, projeto.getId());
        assertThat(relido.getStatus()).isEqualTo(Projeto.StatusProjeto.RASCUNHO);
        assertThat(relido.getItensObjeto()).containsExactly(new ItemObjetoSelecionado(tipoObjetoId, 3));
        assertThat(relido.getCompartimentos()).singleElement()
                .satisfies(x -> assertThat(x.getPosXMm()).isEqualTo(6));
        assertThat(relido.getCompra().getStatus()).isEqualTo(Compra.StatusCompra.FECHADA);
        assertThat(relido.getCompra().getCliente().getEndereco().cidade()).isEqualTo("Sao Paulo");

        Pedido pedidoRelido = em.find(Pedido.class, pedido.getId());
        assertThat(pedidoRelido.getEtapas()).extracting(PedidoEtapa::getNomeEtapa)
                .containsExactly("Compra aprovada");
    }
}
