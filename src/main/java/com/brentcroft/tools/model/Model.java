package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.Materializer;
import org.xml.sax.InputSource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface Model extends Map< String, Object >
{
    Expander getExpander();

    Evaluator getEvaluator();

    default void notifyModelEvent( ModelEvent modelEvent )
    {
        System.out.printf(
                "%s%n",
//                modelEvent.getSource(),
//                modelEvent.getEventType(),
                modelEvent.getMessage() );
    }


    Object set( String key, Object value );

    Object putStatic( String key, Object value );

    Map< String, Object > getStaticModel();

    Stack< Map< String, Object > > getScopeStack();

    /**
     * Expands a value using the expander
     * or else just returns the value.
     *
     * @param value the value to be expanded
     * @return the expanded value
     */
    default String expand( String value )
    {
        final Map< String, Object > bindings = newContainer();
        return Optional
                .ofNullable( getExpander() )
                .map( exp -> exp.apply( value, bindings ) )
                .orElse( value );
    }

    /**
     * Evaluates a value using the evaluator
     * or else returns null.
     *
     * @param value the value to be evaluated
     * @return the evaluated value
     */
    default Object eval( String value )
    {
        final Map< String, Object > bindings = newContainer();

        return Optional
                .ofNullable( getEvaluator() )
                .map( evaluator -> {
                    Object[] lastResult = { null };
                    Model
                            .stepsStream( value )
                            .forEach( step -> lastResult[ 0 ] = getEvaluator().apply( step, bindings ) );
                    return lastResult[ 0 ];
                } )
                .orElse( null );
    }

    Map< String, Object > newContainer();

    static Stream< String > stepsStream( String value )
    {
        String uncommented = Stream
                .of( value.split( "\\s*[\\n\\r]+\\s*" ) )
                .filter( v -> ! v.isEmpty() && ! v.startsWith( "#" ) )
                .map( String::trim )
                .collect( Collectors.joining( " " ) );
        return Stream
                .of( uncommented.split( "\\s*[;]+\\s*" ) );
    }

    static String stepsText( Object text )
    {
        return text instanceof Collection
               ? ( ( Collection< ? > ) text )
                       .stream()
                       .filter( Objects::nonNull )
                       .map( Object::toString )
                       .collect( Collectors.joining( ";\n" ) )
               : text.toString();
    }

    static List< String > stepsList( Object list )
    {
        return list instanceof Collection
               ? ( ( Collection< ? > ) list )
                       .stream()
                       .filter( Objects::nonNull )
                       .map( Object::toString )
                       .collect( Collectors.toList() )
               : Collections.singletonList( list.toString() );
    }

    Object steps( String steps, Map< String, Object > args );

    default Object steps( String steps )
    {
        return steps( steps, new HashMap<>() );
    }

    default Object call( String key, Map< String, Object > args )
    {
        return steps( ( String ) get( key ), args );
    }

    default Object call( String key )
    {
        return call( key, new HashMap<>() );
    }

    String toJson();

    Model getSelf();

    String getName();

    Map< String, Object > getParent();

    void setName( String name );

    void setParent( Map< String, Object > parent );

    Path getCurrentDirectory();

    void setCurrentDirectory( Path directoryPath );

    Class< ? extends Model > getModelClass();

    Model newItemFromJson( String jsonText );

    void introspectEntries();

    File getLocalFile( String filePath );

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
            throw new ModelException( "Failed to create new item.", e );
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
        Model item = newChild( this, jsonText );
        filteredPutAll( item );
        return this;
    }

    default Model appendFromXml( InputSource inputSource )
    {
        Model item = newItem();
        item.setParent( this );

        Optional
                .ofNullable( inputSource.getSystemId() )
                .map( Paths::get )
                .map( Path::getParent )
                .ifPresent( item::setCurrentDirectory );

        Materializer< Model > materializer = new Materializer<>(
                () -> ModelRootTag.DOCUMENT_ROOT,
                () -> item );
        materializer.apply( inputSource );
        filteredPutAll( item );
        return item;
    }

    /**
     * Constructs a new child item using the supplied JSON text
     * assigns to this using the supplied key
     * and then returns this;
     *
     * @param key      the child reference
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
     * <p>
     * Uses <code>eval( path )</code> and raises an exception if the result is not a Model
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
        throw new ModelException( format( "Object at path '%s' is not a Model: '%s'",
                path,
                Optional
                        .ofNullable( node )
                        .map( Object::getClass )
                        .map( Class::getSimpleName )
                        .orElse( null ) ) );
    }

    /**
     * The object path to this item relative to the root item,
     * being the concatenation of the names of the ancestors
     * and this item separated by periods.
     * <p>
     * The root item is not included in any path.
     *
     * @return the object path to this Model item
     */
    default String path()
    {
        // root does not appear in any path
        return Optional
                .ofNullable( getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> ( Model ) p )
                .map( p -> ( nonNull( p.getParent() ) ? format( "%s.%s", p.path(), getName() ) : getName() ) )
                .orElse( "" );
    }

    default Model newChild( Map< String, Object > parent, String jsonText )
    {
        Model item = newItemFromJson( jsonText );
        item.setParent( parent );
        transformMapsToModels( item );
        return item;
    }

    @SuppressWarnings( "unchecked" )
    default void transformMapsToModels( Map< String, Object > item )
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
                    transformMapsToModels( childItem );
                    item.put( key, childItem );
                }
            }
        }
        if ( item instanceof Model )
        {
            ( ( Model ) item ).introspectEntries();
        }
    }

    void maybeDelay();

    interface Expander extends BiFunction< String, Map< String, Object >, String >
    {
    }

    interface Evaluator extends BiFunction< String, Map< String, Object >, Object >
    {
    }
}
