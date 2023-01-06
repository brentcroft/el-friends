package com.brentcroft.tools.model;

import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ModelSteps implements Runnable
{
    private final String steps;
    private final Model model;
    private final boolean inline;

    private static final ThreadLocal<Stack<Model>> stack = ThreadLocal.withInitial( Stack::new );

    public ModelSteps( Model model )
    {
        this.model = model;
        this.steps = Optional
                .ofNullable(model.get("$steps"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] has no value for $steps", model.path())));
        this.inline = false;
    }

    public ModelSteps( Model model, String steps ) {
        this.model = model;
        this.steps = steps;
        this.inline = true;
    }

    private Stack<Model> stack() {
        return stack.get();
    }

    public void run()
    {
        stack().push( model );
        try {
            String indent = IntStream
                    .range(0, stack().size() )
                    .mapToObj( i -> "  " )
                    .collect( Collectors.joining());

            String modelPath = model.path();


            model.logStep(
                    inline
                    ? format("%s%s(inline)", indent, modelPath.isEmpty() ? "" : (modelPath + ":"))
                    : format("%s%s$steps", indent, modelPath.isEmpty() ? "" : (modelPath + ".") )
                     );

            String expandedSteps = model.expand( steps );
            Stream
                    .of(expandedSteps.split( "\\s*[;\\n\\r]+\\s*" ))
                    .map( String::trim )
                    .filter( step -> !step.isEmpty() && !step.startsWith( "#" ) )
                    .peek( step -> model.logStep(format("%s -> %s", indent, step)) )
                    .forEach( model::eval );
        } finally {
            stack().pop();
        }
    }
}
