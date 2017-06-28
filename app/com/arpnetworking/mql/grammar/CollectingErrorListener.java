package com.arpnetworking.mql.grammar;

import com.google.common.collect.Lists;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Collections;
import java.util.List;

/**
 * Created by brandon on 6/27/17.
 */
public class CollectingErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line, final int charPositionInLine, final String msg, final RecognitionException e) {
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
        _errors.add(String.format("Problem at line %d, character %d: %s", line, charPositionInLine, msg));
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(_errors);
    }

    private List<String> _errors = Lists.newArrayList();
}
