package ast;

import types.Type;

/**
 * Expressão. type é o tipo calculado pelo verificador de tipos (CP5); fica
 * null até ele rodar. É o único campo que a análise de tipos acrescenta à
 * hierarquia, e é o que a geração de código vai ler.
 */
public abstract class Expr extends Node {

    /** Tipo da expressão. Type.ERROR quando a expressão tem erro. */
    public Type type;

    protected Expr(int line, int column) {
        super(line, column);
    }
}
