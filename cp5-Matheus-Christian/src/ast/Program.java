package ast;

import java.util.List;

import visitor.Visitor;

/** Program → DeclList $ */
public class Program extends Node {

    public final List<Decl> declarations;

    public Program(int line, int column, List<Decl> declarations) {
        super(line, column);
        this.declarations = declarations;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitProgram(this);
    }
}
