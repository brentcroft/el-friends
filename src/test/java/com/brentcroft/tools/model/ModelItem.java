package com.brentcroft.tools.model;

import com.brentcroft.tools.el.*;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;

public class ModelItem extends AbstractModelItem implements Parented
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();

    static
    {
        ELTemplateManager el = jstl
                .getELTemplateManager();

        el.addPrimaryResolvers(
                new ThreadLocalStackELResolver( el, el, AbstractModelItem.scopeStack ) );

        el.addSecondaryResolvers(
                new ConditionalMethodsELResolver(el.getELContextFactory(), AbstractModelItem.scopeStack),
                new SimpleMapELResolver( AbstractModelItem.staticModel ) );
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings( this );
        bindings.put( "$local", AbstractModelItem.scopeStack.get().peek() );
        bindings.put( "$self", this );
        bindings.put( "$parent", getParent() );
        bindings.put( "$static", AbstractModelItem.staticModel );
        return bindings;
    }

    @Override
    public Expander getExpander()
    {
        return jstl::expandText;
    }

    @Override
    public Evaluator getEvaluator()
    {
        return jstl::eval;
    }
}