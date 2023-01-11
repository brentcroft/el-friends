package com.brentcroft.tools.model;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;

import java.util.Map;
import java.util.Stack;

public class ModelItem extends AbstractModelItem
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();
    private static final ThreadLocal< Stack<Map<String, Object>> > scopeStack = ThreadLocal.withInitial( Stack::new );

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    @Override
    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings(this);
        //
        bindings.put( "$local", bindings );
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

    @Override
    public Map<String, Object> getCurrentScope()
    {
        return scopeStack.get().empty()
               ? newContainer()
               : scopeStack.get().peek();
    }

    @Override
    public void setCurrentScope(Map<String, Object> currentScope) {
        scopeStack.get().push( currentScope );
    }

    @Override
    public void dropCurrentScope() {
        if (! scopeStack.get().empty()) {
            scopeStack.get().pop();
        }
    }
}