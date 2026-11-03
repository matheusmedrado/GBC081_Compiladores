package ast;

/**
 * Comando: bloco, atribuição, chamada, if, while ou return. A declaração
 * local (VarDecl) também aparece num bloco, mas estende Decl e entra no
 * bloco pela interface StmtNode.
 */
public abstract class Stmt extends Node implements StmtNode {

    protected Stmt(int line, int column) {
        super(line, column);
    }
}
