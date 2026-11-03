package ast;

import visitor.Visitor;

/** Param → Type id */
public class Param extends Node {

    public final TypeNode type;
    public final String name;

    public Param(int line, int column, TypeNode type, String name) {
        super(line, column);
        this.type = type;
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitParam(this);
    }
}
