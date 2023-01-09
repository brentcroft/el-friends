# el-friends

[![Maven Central](https://img.shields.io/maven-central/v/com.brentcroft.tools/el-friends.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.brentcroft.tools%22%20AND%20a:%22el-friends%22)

Hierarchical map friendly to EL.

Create a concrete implementation of AbstractModelItem, for example:

```java
package com.brentcroft.tools.model;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;

public class ModelItem extends AbstractModelItem
{
    private static Expander expander;
    private static Evaluator evaluator;

    static {
        JstlTemplateManager jstl = new JstlTemplateManager();
        ModelItem.expander = jstl::expandText;
        ModelItem.evaluator = jstl::eval;
    }

    @Override
    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$self", getSelf() );
        bindings.put( "$parent", getParent() );
        return bindings;
    }

    public Expander getExpander() {
        return expander;
    }
    public Evaluator getEvaluator() {
        return evaluator;
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }
}
```
