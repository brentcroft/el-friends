package com.brentcroft.tools.model;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ModelSteps implements Runnable
{
    private final String steps;
    private final Model model;

    public ModelSteps( Model model )
    {
        this.model = model;

        this.steps = Optional
                .ofNullable(model.get("$steps"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] has no value for $steps", model.path())));
    }

    public void run()
    {
        String expandedSteps = model.expand( steps );
        Stream
                .of(expandedSteps.split( "\\s*[;\\n\\r]+\\s*" ))
                .map( String::trim )
                .filter( step -> !step.isEmpty() && !step.startsWith( "#" ) )
                .peek( model::logStep )
                .forEach( model::eval );
    }
}
