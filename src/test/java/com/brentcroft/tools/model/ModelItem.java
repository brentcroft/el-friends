package com.brentcroft.tools.model;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;
import java.util.Stack;

public class ModelItem extends AbstractModelItem
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$local", getScopeStack().peek() );
        bindings.put( "$self", this );
        bindings.put( "$parent", getParent() );
        bindings.put( "$static", getStaticModel() );
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
}