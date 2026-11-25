package types;

/**
 * Tipo semântico. Os seis tipos da linguagem e o tipo de erro são as únicas
 * instâncias, então comparar tipos é comparar referências (==).
 *
 * A assinatura de uma função não é um Type: fica no Symbol, como o tipo de
 * retorno (Symbol.type, VOID no procedimento) e a lista Symbol.parameters.
 */
public final class Type {

    public static final Type INT = new Type("int");
    public static final Type DOUBLE = new Type("double");
    public static final Type CHAR = new Type("char");
    public static final Type BOOL = new Type("bool");
    public static final Type STRING = new Type("string");
    public static final Type VOID = new Type("void");

    /**
     * Tipo de uma expressão que já gerou mensagem (ou cujo nome não foi
     * declarado). É compatível com tudo e não gera mensagem nova: é assim que
     * o verificador evita a cascata.
     */
    public static final Type ERROR = new Type("erro");

    public final String name;

    private Type(String name) {
        this.name = name;
    }

    /** O tipo de um TypeNode ou do kind de um Literal. Nome desconhecido dá ERROR. */
    public static Type named(String name) {
        if ("int".equals(name)) return INT;
        if ("double".equals(name)) return DOUBLE;
        if ("char".equals(name)) return CHAR;
        if ("bool".equals(name)) return BOOL;
        if ("string".equals(name)) return STRING;
        if ("void".equals(name)) return VOID;
        return ERROR;
    }

    @Override
    public String toString() {
        return name;
    }
}
