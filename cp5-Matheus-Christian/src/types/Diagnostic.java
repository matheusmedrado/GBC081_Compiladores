package types;

/**
 * Uma mensagem da análise contextual: erro (de identificação ou de tipo) ou
 * aviso. O texto já vem no formato "linha L: ..."; o aviso traz "aviso: "
 * logo depois da linha.
 */
public final class Diagnostic {

    public final int line;
    public final String text;
    public final boolean warning;

    public Diagnostic(int line, String text, boolean warning) {
        this.line = line;
        this.text = text;
        this.warning = warning;
    }

    public static Diagnostic error(int line, String message) {
        return new Diagnostic(line, "linha " + line + ": " + message, false);
    }

    public static Diagnostic warning(int line, String message) {
        return new Diagnostic(line, "linha " + line + ": aviso: " + message, true);
    }

    @Override
    public String toString() {
        return text;
    }
}
