# Instruções Obrigatórias para Agentes de IA (Encaixa - Backend)

> **REGRA #1:** Antes de executar qualquer tarefa, propor mudanças ou editar arquivos neste repositório, **leia integralmente o arquivo [`AI_HANDOFF.md`](./AI_HANDOFF.md)**.

## Resumo Rápido de Guardrails
1. **Fonte da Verdade do Banco:** O schema do PostgreSQL é gerenciado exclusivamente pelo Flyway (`src/main/resources/db/migration/V1__schema.sql`). Nunca adicione campos em entidades `@Entity` sem que existam no Flyway (`ddl-auto=validate`).
2. **Colunas JSONB:** `clientes.endereco`, `templates.layout_proporcional` e `projetos.itens_objeto` usam `JsonbConverters.java` + `@ColumnTransformer(write = "?::jsonb")`. Não substitua por `@Embedded` ou `@OneToMany`.
3. **Domínio Geométrico e Precificação:** `EscalaTemplateEngine`, `GeradorLayoutPorObjetos`, `PricingEngine` e `EstimadorPacote` ficam em `br.com.encaixa.domain.engine` (Java puro, sem dependência de Spring/JPA).
4. **Repositório Par (Frontend):** `../encaixa-angular` (veja também `../encaixa-angular/AI_HANDOFF.md`).

Consulte [`AI_HANDOFF.md`](./AI_HANDOFF.md) para ver a lista completa do que já está pronto e os próximos passos do projeto.
