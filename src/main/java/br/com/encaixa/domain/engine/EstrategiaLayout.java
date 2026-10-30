package br.com.encaixa.domain.engine;

/**
 * Disposicao dos compartimentos gerados a partir dos objetos.
 * <ul>
 *   <li>COLUNAS: objetos empilhados em colunas ao longo da largura (padrao).</li>
 *   <li>LINHAS: o mesmo algoritmo com a gaveta "deitada" (objetos em linhas ao longo da profundidade).</li>
 *   <li>*_GIRADO: igual, mas cada objeto e girado 90 graus (largura vira profundidade).</li>
 * </ul>
 * AUTO tenta todas na ordem acima e usa a primeira que couber.
 */
public enum EstrategiaLayout {
    AUTO("Automático", false, false),
    COLUNAS("Em colunas", false, false),
    LINHAS("Em linhas", true, false),
    COLUNAS_GIRADO("Em colunas (objetos girados 90°)", false, true),
    LINHAS_GIRADO("Em linhas (objetos girados 90°)", true, true);

    private final String rotulo;
    private final boolean transpor;
    private final boolean girar;

    EstrategiaLayout(String rotulo, boolean transpor, boolean girar) {
        this.rotulo = rotulo;
        this.transpor = transpor;
        this.girar = girar;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean transpor() {
        return transpor;
    }

    public boolean girar() {
        return girar;
    }

    /** Aceita nulo/vazio/invalido como AUTO. */
    public static EstrategiaLayout de(String valor) {
        if (valor == null || valor.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
