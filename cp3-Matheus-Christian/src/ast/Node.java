package ast;

import visitor.Visitor;

/**
 * Raiz da hierarquia de nós (padrão Interpreter, Aula 7).
 *
 * Guarda a posição do primeiro token que originou o nó. A análise semântica
 * vai reportar erros com essa posição.
 *
 * O único acréscimo do padrão Visitor é o método accept: cada nó concreto
 * chama o método do visitor que tem o seu nome, e a escolha do que fazer é
 * do double dispatch. Nenhum nó e nenhum visitor usa instanceof nem switch
 * sobre o tipo do nó.
 *
 * Os campos symbol (ponteiro para a tabela de símbolos) e type (tipo da
 * expressão) ainda não existem: entram nas aulas seguintes sem mudar a forma
 * da árvore.
 */
public abstract class Node {

    public final int line;
    public final int column;

    protected Node(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public abstract <T> T accept(Visitor<T> visitor);
}
