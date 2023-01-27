package com.brentcroft.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static java.lang.String.format;

@AllArgsConstructor
@Getter
public class ReturnException extends ModelException
{
    private final Object value;

    public String toString()
    {
        return format( "Returning: %s", value );
    }
}
