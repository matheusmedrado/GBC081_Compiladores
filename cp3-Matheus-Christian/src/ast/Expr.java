package ast;

/**
 * Expressão. O campo type (tipo calculado pela análise semântica) entra
 * depois, sem mudar a forma da árvore.
 */
public abstract class Expr extends Node {

    protected Expr(int line, int column) {
        super(line, column);
    }
}
