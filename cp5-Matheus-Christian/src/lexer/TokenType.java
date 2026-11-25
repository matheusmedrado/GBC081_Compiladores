package lexer;

public enum TokenType {
    // identificadores e literais
    IDENTIFIER,
    INT_LITERAL,
    DOUBLE_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,

    // palavras reservadas
    INT, DOUBLE, BOOL, CHAR, STRING, VOID,
    IF, ELSE, WHILE, RETURN,
    TRUE, FALSE,

    // operadores aritméticos
    PLUS,       // +
    MINUS,      // -
    STAR,       // *
    SLASH,      // /
    PERCENT,    // %

    // atribuição, relacionais e lógicos
    ASSIGN,     // =
    EQ,         // ==
    NEQ,        // !=
    LT,         // <
    LE,         // <=
    GT,         // >
    GE,         // >=
    AND,        // &&
    OR,         // ||
    NOT,        // !

    // delimitadores
    LPAREN,     // (
    RPAREN,     // )
    LBRACE,     // {
    RBRACE,     // }
    SEMICOLON,  // ;
    COMMA,      // ,

    EOF
}
