package com.brentcroft.tools.model;

import com.brentcroft.tools.el.ThreadLocalStackELResolver;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.HashMap;
import java.util.Map;

public class ModelItem extends AbstractModelItem
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();

    static {
        jstl
                .getELTemplateManager()
                .addResolver( new ThreadLocalStackELResolver( AbstractModelItem.scopeStack  ) );
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    public Map< String, Object > newContainer()
    {
        final Object local = getScopeStack().isEmpty()
                ? new HashMap<>()
                : getScopeStack().peek();
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$local", local );
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