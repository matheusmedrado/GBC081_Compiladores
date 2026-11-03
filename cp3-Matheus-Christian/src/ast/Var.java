package ast;

import visitor.Visitor;

/** Um nome de uma declaração de variável, com inicialização opcional. */
public class Var extends Node {

    public final String name;
    public final Expr initializer;   // null quando não há '='

    public Var(int line, int column, String name, Expr initializer) {
        super(line, column);
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVar(this);
    }
}
