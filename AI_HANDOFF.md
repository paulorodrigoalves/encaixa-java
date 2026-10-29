# 🧠 AI Context & Handoff (Encaixa - Backend)

> **ATENÇÃO PARA QUALQUER IA LENDO ESTE ARQUIVO:** 
> Este documento contém o contexto arquitetural, regras de negócio e o estado atual do projeto. **Sempre leia este documento antes de propor mudanças arquiteturais ou reescrever entidades.**

## 1. Visão Geral do Sistema
O **Encaixa** é um sistema SaaS B2B/B2C para venda de organizadores de gaveta em acrílico e MDF sob medida (com revestimentos como veludo, courino e linho). 
O motor principal (Layout Engine) calcula dinamicamente a geometria, gerando compartimentos e o orçamento em tempo real. Uma característica vital é o cálculo exato de descontos de espessura baseados no material e tipo de tecido, gerando no final da compra um **Plano de Corte** com as medidas exatas para o marceneiro produzir as peças.

*   **Stack:** Java 21, Spring Boot 3.x, PostgreSQL, Flyway, Keycloak (Auth OAuth2/JWT).
*   **Repositório Frontend Par:** `encaixa-angular` (Angular 19 Standalone + Tailwind).

## 2. Decisões Arquiteturais e Guardrails (CRÍTICO)
*   **Flyway é a Fonte da Verdade:** O banco de dados é estritamente controlado pelos scripts do Flyway (`src/main/resources/db/migration`). **NUNCA** adicione colunas nas entidades JPA (`@Entity`) que não existam no `V1__schema.sql`. O Spring Boot está configurado com `spring.jpa.hibernate.ddl-auto=validate`.
*   **JSONB PostgreSQL:** O campo `endereco` de `Cliente` e `itens_objeto` de `Projeto` são colunas `jsonb` nativas do Postgres. Elas usam um conversor JPA dedicado (`JsonbConverters.java`) manipulando classes internas (`Modelos.Endereco`, etc.). Não transforme essas colunas em entidades com relacionamentos (e.g., `@OneToOne` ou `@Embedded`).
*   **Motor Geométrico (Domain Layer):** O cálculo de divisórias (`EscalaTemplateEngine`, `Geometria`) é código de domínio puro (sem dependências do Spring). Ele calcula **Compartimentos** (os espaços vazios da gaveta).
*   **Conversão para API (Web Layer):** A UI do Angular não desenha os "buracos", ela desenha as paredes físicas. Essa conversão é feita no `LayoutService.java` que transforma a lista de `Compartimento` em `DivisoriaRenderDTO` para a API. O banco salva apenas `Compartimento`.
*   **Status de Domínio:** O enum de status de `Compra` é `FECHADA` ou `APROVADA`. O enum de status de `Projeto` é `RASCUNHO` ou `FECHADO`. O `Pedido` não possui coluna status, ele é derivado do histórico em `PedidoEtapa`.

## 3. Estado Atual (O que já está pronto)
✅ **Infraestrutura:** Docker Compose com PostgreSQL 15 e Keycloak (com import automático do realm `encaixa` e client `encaixa-frontend`).
✅ **Domínio e Testes:** Motor de geometria, motor de precificação e validação das entidades contra o Flyway usando Testcontainers (`PersistenciaSchemaTest.java`).
✅ **API de Catálogo (`CatalogoController`):** Busca materiais, templates e tipos de objetos pré-populados pelo `V2__seed_catalogo.sql`.
✅ **API de Layout e Orçamento (`LayoutController`):** Recebe medidas da gaveta e ID do material + `templateId` OU `itensObjeto`. Roda o motor geométrico (`EscalaTemplateEngine` ou `GeradorLayoutPorObjetos`) e de preços (`PricingEngine`), devolvendo o SVG (`DivisoriaRenderDTO` + `ObjetoPosicionadoDTO`) e orçamento detalhado.
✅ **API de Projetos Autenticada (`ProjetoController` / `ProjetoService`):** Endpoints `POST /api/projetos` e `GET /api/projetos` protegidos por JWT. Suporta salvar projetos tanto por template quanto personalizados por objetos (`projetos_origem_layout`).
✅ **Painel Admin (`AdminController` / `AdminService`):** Endpoints `/api/admin/**` protegidos por `ROLE_ADMIN`. Permite publicar novas regras de precificação (`RegraPrecificacao`), gerenciar o Catálogo (`TipoMaterial`, `Material` e `TipoObjeto` com ativação/desativação), listar compras, aprovar compra gerando `Pedido`, avançar etapas da esteira de produção (`PedidoEtapa`) e gerar o **Plano de Corte para Marcenaria** (`GET /api/admin/compras/{compraId}/plano-corte`).
✅ **Carrinho, Estimador de Pacote e Frete (`CheckoutController` / `CheckoutService`):** Integra `EstimadorPacote` para calcular dimensões e peso da caixa (`GET /api/compras/{id}/cotacao-frete`) e fechar o endereço de entrega e transportadora (`POST /api/compras/{id}/checkout`).

✅ **Pagamento PIX (Mercado Pago) (r.com.encaixa.pagamento):** POST /api/compras/{id}/pagamento/pix gera o QR/copia-e-cola, GET /api/compras/{id}/pagamento consulta/sincroniza, POST /api/public/pagamentos/webhook (público, valida x-signature se MERCADOPAGO_WEBHOOK_SECRET). Pagamento aprovado chama AdminService.aprovarCompraGerarPedido (idempotente). Tabela pagamentos criada em V3__pagamentos.sql. Config por env: MERCADOPAGO_ACCESS_TOKEN, MERCADOPAGO_NOTIFICATION_URL (URL pública, ex. ngrok), MERCADOPAGO_WEBHOOK_SECRET. Sem webhook público, o frontend faz polling a cada 5s.

## 4. Próximos Passos (To-Do / Onde Paramos)
1.  **Notificações de Produção via WhatsApp:** Integrar disparo automático de mensagens ao avançar etapas de pedido.
2.  **Pagamento com cartão / frete real (Melhor Envio):** PIX já pronto; falta cartão e integrar frete real.

## 5. Integrações Futuras / Backlog
*   **Suporte ao Cliente & Notificações via WhatsApp (Gratuito):** Avaliar/utilizar o projeto [WA-AKG](https://github.com/mrifqidaffaaditya/WA-AKG) quando formos implementar a integração de suporte ao cliente e envio de atualizações das etapas do pedido via WhatsApp.

