package ast;

/** Declaração de nível global: variável, procedimento ou função. */
public abstract class Decl extends Node {

    protected Decl(int line, int column) {
        super(line, column);
    }
}
