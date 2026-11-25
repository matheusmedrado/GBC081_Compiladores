package ast;

import symbols.Symbol;
import visitor.Visitor;

/**
 * Uso de identificador numa expressão. decl aponta para o símbolo da
 * declaração; fica null quando o nome não foi declarado.
 */
public class Id extends Expr {

    public final String name;

    /** Símbolo da declaração. Null se a passada ainda não rodou ou o nome não existe. */
    public Symbol decl;

    public Id(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitId(this);
    }
}
