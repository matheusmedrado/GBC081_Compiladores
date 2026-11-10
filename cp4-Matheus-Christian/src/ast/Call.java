package ast;

import java.util.List;

import symbols.Symbol;
import visitor.Visitor;

/**
 * Chamada de função: "fib(n - 1)". O nome chamado é um uso e não é um Id
 * filho, então decl fica neste nó. Os argumentos continuam sendo os filhos.
 */
public class Call extends Expr {

    public final String name;
    public final List<Expr> arguments;

    /** Símbolo da declaração do nome chamado. Null se não foi declarado. */
    public Symbol decl;

    public Call(int line, int column, String name, List<Expr> arguments) {
        super(line, column);
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitCall(this);
    }
}
