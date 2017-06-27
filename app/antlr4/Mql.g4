grammar Mql;

SELECT : [Ss][Ee][Ll][Ee][Cc][Tt] ;
FROM : [Ff][Rr][Oo][Mm] ;

StringLiteral : UnterminatedStringConstant '\'' ;

UnterminatedStringConstant : '\'' ( '\'\'' | ~'\'' )* ;

Identifier : IdentifierCharacter+;

IdentifierCharacter : [a-zA-Z0-9./\\_]
                        |	// these are the valid characters from 0x80 to 0xFF
                        [\u00AA\u00B5\u00BA\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF]
                        |	// these are the letters above 0xFF which only need a single UTF-16 code unit
                        [\u0100-\uD7FF\uE000-\uFFFF] {Character.isLetter((char)_input.LA(-1))}?
                        |	// letters which require multiple UTF-16 code units
                        [\uD800-\uDBFF] [\uDC00-\uDFFF] {Character.isLetter(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
                        ;

COMMA : ',' ;

L_PAREND : '(' ;

R_PAREND : ')' ;

Integral : Digits ;


NumericLiteral : Digits '.' Digits? ([Ee] [+-]? Digits)?
            | '.' Digits ([Ee] [+-]? Digits)?
            | Digits [Ee] [+-]? Digits
            ;

fragment Digits : [0-9]+ ;

Whitespace :[ \t]+ -> channel(HIDDEN) ;

Newline :('\r' '\n'? |'\n') -> channel(HIDDEN) ;

ErrorCharacter : . ;

statement : Select EOF;

Select : SELECT Aggregation? FROM MetricName ;

Aggregation : Identifier (L_PAREND ArgumentList? R_PAREND)? ;

ArgumentList : Argument (COMMA Argument)* ;

Argument : Expression ;

Expression : StringLiteral | NumericLiteral | Identifier ;

MetricName : Identifier ;
