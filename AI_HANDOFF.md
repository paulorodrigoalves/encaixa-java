# 🧠 AI Context & Handoff (Encaixa - Backend)

> **ATENÇÃO PARA QUALQUER IA LENDO ESTE ARQUIVO:** 
> Este documento contém o contexto arquitetural, regras de negócio e o estado atual do projeto. **Sempre leia este documento antes de propor mudanças arquiteturais ou reescrever entidades.**

## 1. Visão Geral do Sistema
O **Encaixa** é um sistema SaaS B2B/B2C para venda de organizadores de gaveta em acrílico sob medida. 
O motor principal (Layout Engine) calcula dinamicamente a geometria, gerando compartimentos e o orçamento em tempo real baseado em regras de precificação por volume e área de acrílico.

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
✅ **API de Layout e Orçamento (`LayoutController`):** Recebe medidas da gaveta e ID do material/template. Roda o motor geométrico (`EscalaTemplateEngine`) e de preços (`PricingEngine`), devolvendo o SVG (`DivisoriaRenderDTO`) e orçamento detalhado.

## 4. Próximos Passos (To-Do / Onde Paramos)
O simulador público e a precificação estão integrados no Frontend. O próximo passo é **Salvar a Gaveta no Banco**.
1.  **Endpoint Seguros:** Criar `ProjetoController` (`POST /api/projetos`) protegido pelo Spring Security (`SecurityConfig` já está setado para JWT).
2.  **Lógica de Salvar (`ProjetoService`):**
    *   Extrair o ID do usuário (Keycloak `sub`), email e nome do `JwtAuthenticationToken`.
    *   Verificar se o `Cliente` existe; se não, cadastrá-lo.
    *   Criar a `Compra` (Rascunho).
    *   Salvar o `Projeto` vinculado à Compra.
    *   Salvar os `Compartimentos` vinculados ao Projeto.
    *   Salvar o `Orcamento` final.
3.  **Layout Dinâmico (Fase 2):** Plugar o `GeradorLayoutPorObjetos` para não depender apenas de templates fixos.
