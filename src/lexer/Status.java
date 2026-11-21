package lexer;

/** Estados S0 a S8 del documento y estado auxiliar STOP. */
public enum Status {
    START,
    IN_IDENTIFIER,
    IN_INTEGER,
    IN_DECIMAL_POINT,
    IN_DECIMAL,
    IN_EXPONENT,
    IN_EXPONENT_SIGN,
    IN_EXPONENT_DIGITS,
    IN_SIMPLE_SYMBOL,
    STOP
}
