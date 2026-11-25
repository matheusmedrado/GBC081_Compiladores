package ast;

import visitor.Visitor;

/**
 * O que pode aparecer dentro de um bloco: um comando (Stmt) ou uma declaração
 * local (VarDecl). VarDecl estende Decl, porque uma declaração de variável
 * pode ser global ou local e é o mesmo nó nos dois lugares, e entra no bloco
 * por esta interface. Os três métodos já vêm de Node, que todo StmtNode
 * estende.
 */
public interface StmtNode {

    int line();

    int column();

    <T> T accept(Visitor<T> visitor);
}
