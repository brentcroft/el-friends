package com.brentcroft.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.lang.String.format;

@AllArgsConstructor
@Getter
public class RanOutOfTriesException extends ModelException
{
    private final int tries;
    private final String test;

    public String toString()
    {
        return format( "Ran out of tries (%s) but: %s", tries, test );
    }
}
