package com.brentcroft.tools.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface Model extends Map<String,Object>
{
    String expand( String value );
    Object eval( String value );
    String toJson();
    Model getSelf();
    String getName();
    Map< String, Object > getParent();
    void setName( String name );
    void setParent( Map< String, Object > parent );
    Class< ? extends Model > getModelClass();
    Model newItemFromJson( String jsonText);
    void introspectEntries();
    void filteredPutAll( Map< ? extends String, ? > item);

    default Model newItem() {
        try
        {
            return getModelClass()
                    .getDeclaredConstructor()
                    .newInstance();
        }
        catch ( InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e )
        {
            throw new IllegalArgumentException("Failed to create new item.", e);
        }
    }

    default Model appendFromJson( String jsonText ) {
        Model item = newChild(this, jsonText);
        filteredPutAll(item);
        return this;
    }

    default Model insertFromJson( String key, String jsonText ) {
        Model item = newChild(this, jsonText);
        item.setName( key );
        put(key, item);
        return item;
    }

    default Model getRoot() {
        return Optional
                .ofNullable( getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> ( Model )p )
                .map( Model::getRoot )
                .orElse( this );
    }

    default Model getItem( String key ) {
        Object node = null;
        for ( String p : key.split("\\s*\\.\\s*") ) {
            if ( node == null ) {
                node = get(p);
            } else if ( node instanceof Map) {
                node = ((Map<?,?>)node).get( p );
            } else if (node instanceof ObjectNode ) {
                node = ((ObjectNode)node).get(p);
            } else {
                throw new IllegalArgumentException(format("Unknown type: '%s' at step '%s' in path '%s'", node, p, key));
            }
        }
        if (node instanceof Model )
        {
            return ( Model )node;
        }
        throw new IllegalArgumentException(format("Object at path '%s' is not a Model: '%s'",
                key,
                Optional
                        .ofNullable(node)
                        .map( Object::getClass )
                        .map( Class::getSimpleName )
                        .orElse( null )));
    }

    default String path() {
        return Optional
                .ofNullable( getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> ( Model )p )
                .filter( p -> nonNull(p.getParent()))
                .map( p -> p.path() + "." + getName())
                .orElse( getName() );
    }

    default Model newChild( Map< String, Object> parent, String jsonText) {
        Model item = newItemFromJson(jsonText);
        item.setParent( parent );
        transformMapsToItems(item);
        return item;
    }


    @SuppressWarnings( "unchecked" )
    default void transformMapsToItems( Map<String,Object> item )
    {
        for (String key : item.keySet() ) {
            Object value = item.get(key);
            if (value instanceof Map) {
                if (value instanceof Model ) {
                    Model childItem = ( Model ) value;
                    childItem.setName( key );
                    childItem.setParent( item );
                } else {
                    Model childItem = newItem();
                    childItem.setName( key );
                    childItem.setParent( item );

                    childItem.putAll( (Map< String, Object> ) value );
                    transformMapsToItems(childItem);
                    childItem.introspectEntries();
                    item.put( key, childItem );
                }
            }
        }
        if ( item instanceof Model )
        {
            (( Model ) item).introspectEntries();
        }
    }
}
