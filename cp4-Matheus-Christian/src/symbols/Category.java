package symbols;

/** Categoria do nome na tabela. A linguagem não tem constante nem parâmetro por referência. */
public enum Category {

    VARIABLE,
    PARAMETER,
    PROCEDURE,
    FUNCTION;

    /** Nome usado no relatório da passada e no desenho do ambiente. */
    public String label() {
        switch (this) {
            case VARIABLE:
                return "variável";
            case PARAMETER:
                return "parâmetro";
            case PROCEDURE:
                return "procedimento";
            case FUNCTION:
                return "função";
            default:
                return name();
        }
    }
}
