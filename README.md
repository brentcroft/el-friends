# el-friends

[![Maven Central](https://img.shields.io/maven-central/v/com.brentcroft.tools/el-friends.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.brentcroft.tools%22%20AND%20a:%22el-friends%22)

Hierarchical map friendly to EL.

Create a concrete implementation of AbstractModelItem, for example:

```java
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
        return Optional
                .ofNullable(evaluator)
                .map(exp -> exp.apply( value, newContainer() ) )
                .orElse( value );
    }

    private Map<String, Object> newContainer() {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$self", getSelf() );
        bindings.put( "$parent", getParent() );
        return bindings;
    }
}
```
