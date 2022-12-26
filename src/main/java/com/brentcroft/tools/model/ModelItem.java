package com.brentcroft.tools.model;

public class ModelItem extends AbstractModelItem
{
    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }
}
