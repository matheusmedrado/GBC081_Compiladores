package ast;

import visitor.Visitor;

/** Type → 'int' | 'double' | 'bool' | 'char' | 'string'. O nome é o lexema. */
public class TypeNode extends Node {

    public final String name;

    public TypeNode(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitTypeNode(this);
    }
}
