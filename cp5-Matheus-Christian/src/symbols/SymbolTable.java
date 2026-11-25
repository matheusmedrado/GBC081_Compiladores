package symbols;

/**
 * Tabela de símbolos da Aula 11. O visitor depende só desta interface.
 *
 * declare não substitui um símbolo que já está no escopo corrente: devolve
 * false e a primeira declaração continua. lookup e lookupLocal devolvem null
 * quando não acham, e nunca lançam exceção. closeScope sem escopo aberto não
 * faz nada (não lança NullPointerException). level 0 é o global; sem nenhum
 * escopo aberto, level devolve -1.
 */
public interface SymbolTable {

    /** Entra num bloco. */
    void openScope();

    /** Sai do bloco. Sem escopo aberto, não faz nada. */
    void closeScope();

    /**
     * Insere s no escopo corrente. Devolve false se o nome já existe neste
     * escopo; nesse caso o símbolo antigo permanece.
     */
    boolean declare(Symbol symbol);

    /** Só no escopo corrente. Null se não existe, ou se não há escopo aberto. */
    Symbol lookupLocal(String name);

    /** Do escopo mais interno para o mais externo. Null se não existe. */
    Symbol lookup(String name);

    /** Profundidade atual. 0 é o global; -1 se não há escopo aberto. */
    int level();
}
