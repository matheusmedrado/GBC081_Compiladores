package types;

/**
 * As regras de tipos de tipos-design.md, sem AST: recebem tipos e devolvem
 * tipos. Cada método cita a regra do papel. null quer dizer "a regra não se
 * aplica" (o verificador transforma isso em mensagem); ERROR num operando
 * devolve ERROR, sem mensagem.
 *
 * Hierarquia numérica (decisões 1 e 2): char < int < double. Um valor pode
 * subir nessa ordem sem conversão explícita; nunca desce.
 */
public final class TypeRules {

    private TypeRules() {
    }

    /** char, int ou double. */
    public static boolean isNumeric(Type t) {
        return t == Type.CHAR || t == Type.INT || t == Type.DOUBLE;
    }

    /** int ou char (char sobe para int). */
    public static boolean isIntegral(Type t) {
        return t == Type.CHAR || t == Type.INT;
    }

    /**
     * Tipo do resultado de uma operação aritmética entre dois numéricos:
     * double se algum for double, senão int. char + char é int (decisão 2).
     */
    static Type arithmeticResult(Type a, Type b) {
        return a == Type.DOUBLE || b == Type.DOUBLE ? Type.DOUBLE : Type.INT;
    }

    /**
     * [COMPAT] Um valor do tipo source pode ser guardado onde se espera target?
     * Vale para atribuição, inicializador, argumento e return.
     * Igual: sim. char → int, char → double, int → double: sim (alargamento).
     * double → int, int → char, double → char e qualquer outro par: não.
     */
    public static boolean assignable(Type target, Type source) {
        if (target == Type.ERROR || source == Type.ERROR) {
            return true;
        }
        if (target == source) {
            return target != Type.VOID;
        }
        if (target == Type.INT) {
            return source == Type.CHAR;
        }
        if (target == Type.DOUBLE) {
            return source == Type.CHAR || source == Type.INT;
        }
        return false;
    }

    /**
     * Operador binário. Devolve o tipo do resultado, ERROR se um operando
     * já é ERROR, ou null se a tabela de operadores recusa o par.
     */
    public static Type binary(String op, Type left, Type right) {
        if (left == Type.ERROR || right == Type.ERROR) {
            return Type.ERROR;
        }
        if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op)) {
            // [ARIT] e1 : n1, e2 : n2, n1 e n2 numéricos ⊢ e1 op e2 : max(int, n1, n2)
            return isNumeric(left) && isNumeric(right) ? arithmeticResult(left, right) : null;
        }
        if ("%".equals(op)) {
            // [MOD] e1, e2 : int ou char ⊢ e1 % e2 : int
            return isIntegral(left) && isIntegral(right) ? Type.INT : null;
        }
        if ("<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
            // [REL] e1, e2 numéricos ⊢ e1 op e2 : bool
            return isNumeric(left) && isNumeric(right) ? Type.BOOL : null;
        }
        if ("==".equals(op) || "!=".equals(op)) {
            // [IGUAL-NUM] e1, e2 numéricos ⊢ e1 op e2 : bool
            // [IGUAL-BOOL] e1, e2 : bool ⊢ e1 op e2 : bool
            // string, e pares misturados com bool, são recusados (decisões 4 e 5).
            if (isNumeric(left) && isNumeric(right)) {
                return Type.BOOL;
            }
            return left == Type.BOOL && right == Type.BOOL ? Type.BOOL : null;
        }
        if ("&&".equals(op) || "||".equals(op)) {
            // [LOG] e1, e2 : bool ⊢ e1 op e2 : bool
            return left == Type.BOOL && right == Type.BOOL ? Type.BOOL : null;
        }
        return null;
    }

    /** Operador unário. Mesma convenção de binary. */
    public static Type unary(String op, Type operand) {
        if (operand == Type.ERROR) {
            return Type.ERROR;
        }
        if ("-".equals(op)) {
            // [NEG] e : n numérico ⊢ -e : max(int, n)
            return isNumeric(operand) ? arithmeticResult(operand, Type.INT) : null;
        }
        if ("!".equals(op)) {
            // [NOT] e : bool ⊢ !e : bool
            return operand == Type.BOOL ? Type.BOOL : null;
        }
        return null;
    }

    /** [IF] e [WHILE]: a condição precisa ser bool. ERROR passa, sem mensagem. */
    public static boolean isCondition(Type t) {
        return t == Type.BOOL || t == Type.ERROR;
    }
}
