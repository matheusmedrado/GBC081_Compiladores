package ast;

import symbols.Symbol;
import visitor.Visitor;

/**
 * Atribuição: "x = expr;". Atribuição é comando, não expressão.
 * O nome da esquerda é um uso: não há nó Id filho, então decl fica aqui.
 */
public class Assign extends Stmt {

    public final String name;
    public final Expr value;

    /** Símbolo da declaração do nome. Null se não foi declarado. */
    public Symbol decl;

    public Assign(int line, int column, String name, Expr value) {
        super(line, column);
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAssign(this);
    }
}
