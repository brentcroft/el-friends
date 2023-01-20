# el-model

[![Maven Central](https://img.shields.io/maven-central/v/com.brentcroft.tools/el-model.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.brentcroft.tools%22%20AND%20a:%22el-model%22)

Hierarchical map friendly to EL.

Create a concrete implementation of AbstractModelItem, for example:

```java
package com.brentcroft.tools.model;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;

public class ModelItem extends AbstractModelItem
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();

    @Override
    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$self", getSelf() );
        bindings.put( "$parent", getParent() );
        return bindings;
    }

    @Override
    public Expander getExpander() {
        return jstl::expandText;
    }

    @Override
    public Evaluator getEvaluator() {
        return jstl::eval;
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }
}
```
