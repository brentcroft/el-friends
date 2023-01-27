package com.brentcroft.tools.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ModelException extends RuntimeException
{
    ModelException( String message )
    {
        super( message );
    }

    ModelException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
