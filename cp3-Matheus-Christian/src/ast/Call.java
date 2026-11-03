package ast;

import java.util.List;

import visitor.Visitor;

/** Chamada de função dentro de uma expressão: "fib(n - 1)". */
public class Call extends Expr {

    public final String name;
    public final List<Expr> arguments;

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
