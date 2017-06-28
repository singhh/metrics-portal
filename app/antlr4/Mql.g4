grammar Mql;

PIPE : '|' ;
SELECT : [Ss][Ee][Ll][Ee][Cc][Tt] ;
FROM : [Ff][Rr][Oo][Mm] ;
TO : [Tt][Oo] ;
AS : [Aa][Ss] ;
NOW : [Nn][Oo][Ww] ;
AGO : [Aa][Gg][Oo] ;
GROUP : [Gg][Rr][Oo][Uu][Pp] ;
BY : [Bb][Yy] ;

SECONDS : [Ss][Ee][Cc][Oo][Nn][Dd][Ss] ;
SECOND : [Ss][Ee][Cc][Oo][Nn][Dd] ;
MINUTES : [Mm][Ii][Nn][Uu][Tt][Ee][Ss] ;
MINUTE : [Mm][Ii][Nn][Uu][Tt][Ee] ;
HOURS : [Hh][Oo][Uu][Rr][Ss] ;
HOUR : [Hh][Oo][Uu][Rr] ;
DAYS : [Dd][Aa][Yy][Ss] ;
DAY : [Dd][Aa][Yy] ;
WEEKS : [Ww][Ee][Ee][Kk][Ss] ;
WEEK : [Ww][Ee][Ee][Kk] ;
MONTHS : [Mm][Oo][Nn][Tt][Hh][Ss] ;
MONTH : [Mm][Oo][Nn][Tt][Hh] ;
YEARS : [Yy][Ee][Aa][Rr][Ss] ;
YEAR : [Yy][Ee][Aa][Rr] ;

COMMA : ',' ;
L_PAREND : '(' ;
R_PAREND : ')' ;

NumericLiteral : Digits '.' Digits? ([Ee] [+-]? Digits)?
            | '.' Digits ([Ee] [+-]? Digits)?
            | Digits [Ee] [+-]? Digits
            | Integral
            ;

Integral : Digits ;

fragment Digits : [0-9]+ ;

timeUnit : SECONDS | SECOND | MINUTES | MINUTE | HOURS | HOUR | DAYS | DAY | WEEKS | WEEK | MONTHS | MONTH | YEARS | YEAR ;

StringLiteral : UnterminatedStringConstant '\'' ;

UnterminatedStringConstant : '\'' ( '\'\'' | ~'\'' )* ;

Whitespace :[ \t]+ -> channel(HIDDEN) ;

Newline :('\r' '\n'? | '\n') -> channel(HIDDEN) ;

Identifier : IdentifierCharacter+;

IdentifierCharacter : [a-zA-Z0-9./\\_]
                        |	// these are the valid characters from 0x80 to 0xFF
                        [\u00AA\u00B5\u00BA\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF]
                        |	// these are the letters above 0xFF which only need a single UTF-16 code unit
                        [\u0100-\uD7FF\uE000-\uFFFF] {Character.isLetter((char)_input.LA(-1))}?
                        |	// letters which require multiple UTF-16 code units
                        [\uD800-\uDBFF] [\uDC00-\uDFFF] {Character.isLetter(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
                        ;

ErrorCharacter : . ;

statement : stage (PIPE stage)* EOF;

stage : select ;

select : FROM timeRange SELECT metricName groupByClause? (AS? Identifier)?;

groupByClause : GROUP BY groupByTerm (COMMA groupByTerm)* ;

groupByTerm : Identifier ;

aggregation : Identifier (L_PAREND argumentList? R_PAREND)? ;

argumentList : argument (COMMA argument)* ;

argument : expression ;

expression : StringLiteral | NumericLiteral | Identifier ;

timeRange : pointInTime (TO pointInTime)? ;

pointInTime : relativeTime | absoluteTime ;

relativeTime : NOW | NumericLiteral timeUnit AGO ;

absoluteTime : StringLiteral ;

metricName : Identifier ;
