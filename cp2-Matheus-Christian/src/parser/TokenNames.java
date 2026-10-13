package parser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lexer.Token;
import lexer.TokenType;

import static lexer.TokenType.*;

/** Textos das mensagens de erro: como mostrar um token e um conjunto de tokens esperados. */
final class TokenNames {

    private TokenNames() {
    }

    private static final Set<TokenType> BINARY_OPERATORS = EnumSet.of(
            PLUS, MINUS, STAR, SLASH, PERCENT, EQ, NEQ, LT, LE, GT, GE, AND, OR);

    /**
     * Grupos usados para encurtar a lista de esperados. Um grupo só aparece
     * quando o conjunto contém todos os tokens dele, então a mensagem continua
     * dizendo exatamente o conjunto que o parser testou.
     */
    private static final Map<String, Set<TokenType>> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("comando", Parser.FIRST_STMT);
        GROUPS.put("declaração", Parser.FIRST_DECL);
        GROUPS.put("expressão", Parser.FIRST_EXPR);
        GROUPS.put("operador", BINARY_OPERATORS);
        GROUPS.put("tipo", Parser.TYPES);
    }

    /** Texto fixo de cada tipo de token, como aparece no fonte. */
    static String lexeme(TokenType type) {
        switch (type) {
            case PLUS:      return "+";
            case MINUS:     return "-";
            case STAR:      return "*";
            case SLASH:     return "/";
            case PERCENT:   return "%";
            case ASSIGN:    return "=";
            case EQ:        return "==";
            case NEQ:       return "!=";
            case LT:        return "<";
            case LE:        return "<=";
            case GT:        return ">";
            case GE:        return ">=";
            case AND:       return "&&";
            case OR:        return "||";
            case NOT:       return "!";
            case LPAREN:    return "(";
            case RPAREN:    return ")";
            case LBRACE:    return "{";
            case RBRACE:    return "}";
            case SEMICOLON: return ";";
            case COMMA:     return ",";
            default:        return type.name().toLowerCase();   // palavras reservadas
        }
    }

    /** Nome de um tipo de token numa mensagem ("identificador", "';'"...). */
    static String describe(TokenType type) {
        switch (type) {
            case IDENTIFIER:     return "identificador";
            case INT_LITERAL:    return "literal inteiro";
            case DOUBLE_LITERAL: return "literal double";
            case STRING_LITERAL: return "literal string";
            case CHAR_LITERAL:   return "literal de caractere";
            case EOF:            return "fim do arquivo";
            default:             return "'" + lexeme(type) + "'";
        }
    }

    /** Um token encontrado no fonte: o lexema entre aspas, ou "fim do arquivo". */
    static String describe(Token token) {
        return token.type == EOF ? "fim do arquivo" : "'" + token.lexeme + "'";
    }

    /**
     * Conjunto de esperados: grupos inteiros primeiro, depois os tokens soltos
     * na ordem do enum. Um grupo já coberto por outro ("tipo" dentro de
     * "comando") não é repetido.
     */
    static String describe(Set<TokenType> expected) {
        List<String> parts = new ArrayList<>();
        Set<TokenType> covered = EnumSet.noneOf(TokenType.class);
        for (Map.Entry<String, Set<TokenType>> group : GROUPS.entrySet()) {
            if (expected.containsAll(group.getValue()) && !covered.containsAll(group.getValue())) {
                parts.add(group.getKey());
                covered.addAll(group.getValue());
            }
        }
        for (TokenType type : expected) {
            if (!covered.contains(type)) {
                parts.add(describe(type));
            }
        }
        return joinWithOr(parts);
    }

    private static String joinWithOr(List<String> parts) {
        if (parts.size() == 1) {
            return parts.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(i == parts.size() - 1 ? " ou " : ", ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
