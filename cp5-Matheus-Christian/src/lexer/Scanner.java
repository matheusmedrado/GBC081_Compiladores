package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analisador léxico escrito à mão a partir do AFD da linguagem.
 *
 * Cada constante de State é um estado do autômato (mesmos nomes dos desenhos
 * em automatos/) e cada estado é um case do switch em nextToken().
 * Erros léxicos são guardados em getErrors() e o scanner continua a partir do
 * próximo caractere, então nextToken() só devolve tokens válidos ou EOF.
 */
public class Scanner {

    enum State {
        START,
        IDENT,
        NUM_INT, NUM_DOT, NUM_FRAC,
        STR, STR_ESC,
        CHR, CHR_ESC, CHR_END, CHR_LONG,
        SLASH, LINE_COMMENT, BLOCK_COMMENT, BLOCK_COMMENT_STAR,
        ASSIGN, BANG, LESS, GREATER, AMP, PIPE
    }

    public static final int MAX_IDENT_LENGTH = 32;

    private static final int EOF = -1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("double", TokenType.DOUBLE);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("char", TokenType.CHAR);
        KEYWORDS.put("string", TokenType.STRING);
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
    }

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    // onde começa o token (ou erro) que está sendo lido
    private int startPos;
    private int startLine;
    private int startColumn;

    private final List<LexicalError> errors = new ArrayList<>();

    public Scanner(String source) {
        this.source = source;
    }

    public List<LexicalError> getErrors() {
        return errors;
    }

    /** Lê todos os tokens até o EOF (incluído no final da lista). */
    public List<Token> scanAll() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.type != TokenType.EOF);
        return tokens;
    }

    // ---------------------------------------------------------------
    // cursor sobre o código-fonte
    // ---------------------------------------------------------------

    private boolean hasNext() {
        return pos < source.length();
    }

    /** Caractere atual sem consumir, ou EOF. */
    private int peek() {
        return hasNext() ? source.charAt(pos) : EOF;
    }

    /** Consome o caractere atual e atualiza linha/coluna. */
    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    // ---------------------------------------------------------------
    // AFD
    // ---------------------------------------------------------------

    /**
     * Devolve o próximo token usando maximal munch: o autômato só para de
     * consumir quando o próximo caractere não tem transição a partir do
     * estado atual. Nesse ponto, se o estado é de aceitação, o token é
     * emitido sem consumir esse caractere.
     */
    public Token nextToken() {
        State state = State.START;

        while (true) {
            int c = peek();

            switch (state) {

                case START:
                    markStart();
                    if (c == EOF) {
                        return makeToken(TokenType.EOF);
                    }
                    advance();
                    if (isWhitespace(c)) {
                        break;                          // descarta e continua em START
                    }
                    if (isLetter(c)) {
                        state = State.IDENT;
                        break;
                    }
                    if (isDigit(c)) {
                        state = State.NUM_INT;
                        break;
                    }
                    switch (c) {
                        case '"':  state = State.STR; break;
                        case '\'': state = State.CHR; break;
                        case '/':  state = State.SLASH; break;
                        case '=':  state = State.ASSIGN; break;
                        case '!':  state = State.BANG; break;
                        case '<':  state = State.LESS; break;
                        case '>':  state = State.GREATER; break;
                        case '&':  state = State.AMP; break;
                        case '|':  state = State.PIPE; break;
                        case '+':  return makeToken(TokenType.PLUS);
                        case '-':  return makeToken(TokenType.MINUS);
                        case '*':  return makeToken(TokenType.STAR);
                        case '%':  return makeToken(TokenType.PERCENT);
                        case '(':  return makeToken(TokenType.LPAREN);
                        case ')':  return makeToken(TokenType.RPAREN);
                        case '{':  return makeToken(TokenType.LBRACE);
                        case '}':  return makeToken(TokenType.RBRACE);
                        case ';':  return makeToken(TokenType.SEMICOLON);
                        case ',':  return makeToken(TokenType.COMMA);
                        case '_':
                            error("identificador não pode começar com '_'");
                            break;
                        default:
                            // caractere fora do alfabeto: já foi consumido, segue em START
                            error("caractere inválido " + describe(c));
                    }
                    break;

                // ---------- identificador / palavra reservada ----------

                case IDENT:
                    if (isLetter(c) || isDigit(c) || c == '_') {
                        advance();
                        break;
                    }
                    String word = currentLexeme();
                    if (word.length() > MAX_IDENT_LENGTH) {
                        error("identificador com mais de " + MAX_IDENT_LENGTH + " caracteres");
                        state = State.START;
                        break;
                    }
                    return makeToken(KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER));

                // ---------- literal numérico ----------

                case NUM_INT:
                    if (isDigit(c)) {
                        advance();
                        break;
                    }
                    if (c == '.') {
                        advance();
                        state = State.NUM_DOT;
                        break;
                    }
                    return makeToken(TokenType.INT_LITERAL);

                case NUM_DOT:
                    if (isDigit(c)) {
                        advance();
                        state = State.NUM_FRAC;
                        break;
                    }
                    error("número malformado '" + currentLexeme() + "': falta dígito depois do ponto");
                    state = State.START;
                    break;

                case NUM_FRAC:
                    if (isDigit(c)) {
                        advance();
                        break;
                    }
                    return makeToken(TokenType.DOUBLE_LITERAL);

                // ---------- string ----------

                case STR:
                    if (c == '"') {
                        advance();
                        return makeToken(TokenType.STRING_LITERAL);
                    }
                    if (c == '\\') {
                        advance();
                        state = State.STR_ESC;
                        break;
                    }
                    if (c == '\n' || c == EOF) {
                        // o \n não é consumido: START trata ele como espaço em branco
                        error("string não fechada antes do " + endName(c));
                        state = State.START;
                        break;
                    }
                    advance();
                    break;

                case STR_ESC:
                    if (isEscapable(c)) {
                        advance();
                        state = State.STR;
                        break;
                    }
                    if (c == '\n' || c == EOF) {
                        error("string não fechada antes do " + endName(c));
                        state = State.START;
                        break;
                    }
                    // a barra ficou uma coluna antes do caractere atual
                    error("sequência de escape inválida '\\" + (char) c + "'", line, column - 1);
                    advance();
                    state = State.STR;
                    break;

                // ---------- literal de caractere ----------

                case CHR:
                    if (c == '\'') {
                        advance();
                        error("literal de caractere vazio");
                        state = State.START;
                        break;
                    }
                    if (c == '\\') {
                        advance();
                        state = State.CHR_ESC;
                        break;
                    }
                    if (c == '\n' || c == EOF) {
                        error("literal de caractere não fechado");
                        state = State.START;
                        break;
                    }
                    advance();
                    state = State.CHR_END;
                    break;

                case CHR_ESC:
                    if (isEscapable(c)) {
                        advance();
                        state = State.CHR_END;
                        break;
                    }
                    if (c == '\n' || c == EOF) {
                        error("literal de caractere não fechado");
                        state = State.START;
                        break;
                    }
                    error("sequência de escape inválida '\\" + (char) c + "'", line, column - 1);
                    advance();
                    state = State.CHR_END;
                    break;

                case CHR_END:
                    if (c == '\'') {
                        advance();
                        return makeToken(TokenType.CHAR_LITERAL);
                    }
                    if (c == '\n' || c == EOF) {
                        error("literal de caractere não fechado");
                        state = State.START;
                        break;
                    }
                    advance();
                    state = State.CHR_LONG;
                    break;

                case CHR_LONG:
                    // recuperação: descarta até a aspa que fecha ou até o fim da linha
                    if (c == '\'') {
                        advance();
                        error("literal de caractere com mais de um caractere");
                        state = State.START;
                        break;
                    }
                    if (c == '\n' || c == EOF) {
                        error("literal de caractere não fechado");
                        state = State.START;
                        break;
                    }
                    advance();
                    break;

                // ---------- barra: divisão ou comentário ----------

                case SLASH:
                    if (c == '/') {
                        advance();
                        state = State.LINE_COMMENT;
                        break;
                    }
                    if (c == '*') {
                        advance();
                        state = State.BLOCK_COMMENT;
                        break;
                    }
                    return makeToken(TokenType.SLASH);

                case LINE_COMMENT:
                    if (c == '\n' || c == EOF) {
                        state = State.START;
                        break;
                    }
                    advance();
                    break;

                case BLOCK_COMMENT:
                    if (c == '*') {
                        advance();
                        state = State.BLOCK_COMMENT_STAR;
                        break;
                    }
                    if (c == EOF) {
                        error("comentário de bloco não fechado");
                        state = State.START;
                        break;
                    }
                    advance();
                    break;

                case BLOCK_COMMENT_STAR:
                    if (c == '/') {
                        advance();
                        state = State.START;
                        break;
                    }
                    if (c == '*') {
                        advance();
                        break;
                    }
                    if (c == EOF) {
                        error("comentário de bloco não fechado");
                        state = State.START;
                        break;
                    }
                    advance();
                    state = State.BLOCK_COMMENT;
                    break;

                // ---------- operadores de um ou dois caracteres ----------

                case ASSIGN:
                    if (c == '=') {
                        advance();
                        return makeToken(TokenType.EQ);
                    }
                    return makeToken(TokenType.ASSIGN);

                case BANG:
                    if (c == '=') {
                        advance();
                        return makeToken(TokenType.NEQ);
                    }
                    return makeToken(TokenType.NOT);

                case LESS:
                    if (c == '=') {
                        advance();
                        return makeToken(TokenType.LE);
                    }
                    return makeToken(TokenType.LT);

                case GREATER:
                    if (c == '=') {
                        advance();
                        return makeToken(TokenType.GE);
                    }
                    return makeToken(TokenType.GT);

                case AMP:
                    if (c == '&') {
                        advance();
                        return makeToken(TokenType.AND);
                    }
                    error("'&' sozinho não é operador, o correto é '&&'");
                    state = State.START;
                    break;

                case PIPE:
                    if (c == '|') {
                        advance();
                        return makeToken(TokenType.OR);
                    }
                    error("'|' sozinho não é operador, o correto é '||'");
                    state = State.START;
                    break;
            }
        }
    }

    // ---------------------------------------------------------------
    // auxiliares
    // ---------------------------------------------------------------

    private void markStart() {
        startPos = pos;
        startLine = line;
        startColumn = column;
    }

    private String currentLexeme() {
        return source.substring(startPos, pos);
    }

    private Token makeToken(TokenType type) {
        return new Token(type, currentLexeme(), startLine, startColumn);
    }

    /** Erro na posição onde o token atual começou. */
    private void error(String message) {
        error(message, startLine, startColumn);
    }

    private void error(String message, int line, int column) {
        errors.add(new LexicalError(message, line, column));
    }

    private static boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /** Caracteres aceitos depois de '\' em strings e chars. */
    private static boolean isEscapable(int c) {
        return c == 'n' || c == 't' || c == '\\' || c == '"' || c == '\'';
    }

    private static String endName(int c) {
        return c == EOF ? "fim do arquivo" : "fim da linha";
    }

    private static String describe(int c) {
        if (c < 32 || c == 127) {
            return String.format("(código %d)", c);
        }
        return "'" + (char) c + "'";
    }
}
