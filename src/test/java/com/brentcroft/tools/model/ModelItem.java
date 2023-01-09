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