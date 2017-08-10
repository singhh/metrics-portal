package com.arpnetworking.mql.grammar;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ExecutionException extends Exception {
    public ExecutionException(final List<String> problems) {
        super(StringUtils.join(problems, ", "));
        _problems = problems;
    }

    public List<String> getProblems() {
        return _problems;
    }

    private final List<String> _problems;
    private static final long serialVersionUID = 1L;
}
