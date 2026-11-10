package symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Pilha de mapas: cada escopo aberto é um mapa de nome para símbolo.
 * openScope empilha, closeScope desempilha, lookup desce do topo até achar.
 * O nível é o tamanho da pilha menos um, então não precisa consultar
 * Symbol.level para achar o escopo (isso seria da tabela única com registro).
 */
public class StackedSymbolTable implements SymbolTable {

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<Map<String, Symbol>>();

    @Override
    public void openScope() {
        scopes.push(new HashMap<String, Symbol>());
    }

    @Override
    public void closeScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    @Override
    public boolean declare(Symbol symbol) {
        Map<String, Symbol> current = scopes.peek();
        if (current == null) {
            throw new IllegalStateException("declare sem escopo aberto");
        }
        if (current.containsKey(symbol.name)) {
            return false;
        }
        current.put(symbol.name, symbol);
        return true;
    }

    @Override
    public Symbol lookupLocal(String name) {
        Map<String, Symbol> current = scopes.peek();
        if (current == null) {
            return null;
        }
        return current.get(name);
    }

    @Override
    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    @Override
    public int level() {
        return scopes.size() - 1;
    }
}
