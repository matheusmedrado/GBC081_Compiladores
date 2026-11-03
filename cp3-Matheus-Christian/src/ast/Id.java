package ast;

import visitor.Visitor;

/**
 * Uso de identificador. O ponteiro para o símbolo na tabela de símbolos
 * entra na aula da tabela, sem mudar a forma da árvore.
 */
public class Id extends Expr {

    public final String name;

    public Id(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitId(this);
    }
}
