package ast;

import java.util.List;

import visitor.Visitor;

/**
 * Declaração de procedimento ou função. returnType é null quando o retorno
 * é void (procedimento).
 */
public class FuncDecl extends Decl {

    public final TypeNode returnType;
    public final String name;
    public final List<Param> parameters;
    public final Block body;

    public FuncDecl(int line, int column, TypeNode returnType, String name,
                    List<Param> parameters, Block body) {
        super(line, column);
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitFuncDecl(this);
    }
}
