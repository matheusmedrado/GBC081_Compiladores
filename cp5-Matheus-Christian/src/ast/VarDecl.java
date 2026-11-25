package ast;

import java.util.List;

import visitor.Visitor;

/**
 * Declaração de variáveis, global ou local: "int a, b = 2, c;".
 * Uma declaração com vários nomes é um nó só, com uma entrada por nome.
 */
public class VarDecl extends Decl implements StmtNode {

    public final TypeNode type;
    public final List<Var> variables;

    public VarDecl(int line, int column, TypeNode type, List<Var> variables) {
        super(line, column);
        this.type = type;
        this.variables = variables;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitVarDecl(this);
    }
}
