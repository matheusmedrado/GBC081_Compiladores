package ast;

import java.util.List;

import visitor.Visitor;

/** Block → '{' StmtList '}' */
public class Block extends Stmt {

    public final List<StmtNode> statements;

    public Block(int line, int column, List<StmtNode> statements) {
        super(line, column);
        this.statements = statements;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
