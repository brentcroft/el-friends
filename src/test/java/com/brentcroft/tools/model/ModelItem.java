package com.brentcroft.tools.model;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class ModelItem extends AbstractModelItem
{
    private static BiFunction<String, Map<String,Object>, String> expander;
    private static BiFunction<String, Map<String,Object>, Object> evaluator;

    static {
        JstlTemplateManager jstl = new JstlTemplateManager();
        ModelItem.expander = jstl::expandText;
        ModelItem.evaluator = jstl::eval;
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    /**
     * Expands a value using the expander
     * or else just returns the value.
     *
     * @param value the value to be expanded
     * @return the expanded value
     */
    @Override
    public String expand( String value )
    {
        return Optional
                .ofNullable(expander)
                .map(exp -> exp.apply( value, newContainer() ) )
                .orElse( value );
    }
    /**
     * Evaluates a value using the evaluator
     * or else just returns the value.
     *
     * @param value the value to be evaluated
     * @return the evaluated value
     */
    @Override
    public Object eval( String value )
    {
        if (evaluator == null) {
            return null;
        }
        Map<String, Object> bindings = newContainer();
        Object[] lastResult = {null};
        Model
                .stepsStream( value )
                .forEach( step -> lastResult[0] = evaluator.apply( step, bindings ) );
        return lastResult[0];
    }

    private Map<String, Object> newContainer() {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$self", getSelf() );
        bindings.put( "$parent", getParent() );
        return bindings;
    }
}