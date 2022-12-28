package com.brentcroft.tools.model;

import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface Model extends Map< String, Object >
{
    Object eval( String value );

    default String expand( String value )
    {
        return value;
    }

    String toJson();

    Model getSelf();

    String getName();

    Map< String, Object > getParent();

    void setName( String name );

    void setParent( Map< String, Object > parent );

    Class< ? extends Model > getModelClass();

    Model newItemFromJson( String jsonText );

    void introspectEntries();

    String readFileFully( String filePath );

    void filteredPutAll( Map< ? extends String, ? > item );

    default Model newItem()
    {
        try
        {
            return getModelClass()
                    .getDeclaredConstructor()
                    .newInstance();
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Failed to create new item.", e );
        }
    }

    /**
     * Constructs a temporary sibling item using the supplied JSON text
     * and filters the sibling item's entries into this
     * and then returns this;
     *
     * @param jsonText JSON text to construct a new Model
     * @return this
     */
    default Model appendFromJson( String jsonText )
    {
        Model item = newChild( getParent(), jsonText );
        filteredPutAll( item );
        return this;
    }

    /**
     * Constructs a new child item using the supplied JSON text
     * assigns to this using the supplied key
     * and then returns this;
     *
     * @param key the child reference
     * @param jsonText JSON text to construct a new Model
     * @return this
     */
    default Model insertFromJson( String key, String jsonText )
    {
        Model item = newChild( this, jsonText );
        item.setName( key );
        put( key, item );
        return this;
    }

    default Model getRoot()
    {
        return Optional
                .ofNullable( getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> ( Model ) p )
                .map( Model::getRoot )
                .orElse( this );
    }

    /**
     * Navigate the supplied object path starting from this.
     *
     * Uses <code>eval( path )</code> and raises an exceptiono if the result is not a Model
     *
     * @param path an object path
     * @return a Model
     */
    default Model getItem( String path )
    {
        Object node = eval( path );
        if ( node instanceof Model )
        {
            return ( Model ) node;
        }
        throw new IllegalArgumentException( format( "Object at path '%s' is not a Model: '%s'",
                path,
                Optional
                        .ofNullable( node )
                        .map( Object::getClass )
                        .map( Class::getSimpleName )
                        .orElse( null ) ) );
    }

    default String path()
    {
        return Optional
                .ofNullable( getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> ( Model ) p )
                .filter( p -> nonNull( p.getParent() ) )
                .map( p -> p.path() + "." + getName() )
                .orElse( getName() );
    }

    default Model newChild( Map< String, Object > parent, String jsonText )
    {
        Model item = newItemFromJson( jsonText );
        item.setParent( parent );
        transformMapsToItems( item );
        return item;
    }

    @SuppressWarnings( "unchecked" )
    default void transformMapsToItems( Map< String, Object > item )
    {
        for ( String key : item.keySet() )
        {
            Object value = item.get( key );
            if ( value instanceof Map )
            {
                if ( value instanceof Model )
                {
                    Model childItem = ( Model ) value;
                    childItem.setName( key );
                    childItem.setParent( item );
                }
                else
                {
                    Model childItem = newItem();
                    childItem.setName( key );
                    childItem.setParent( item );
                    childItem.putAll( ( Map< String, Object > ) value );
                    transformMapsToItems( childItem );
                    item.put( key, childItem );
                }
            }
        }
        if ( item instanceof Model )
        {
            ( ( Model ) item ).introspectEntries();
        }
    }
}
