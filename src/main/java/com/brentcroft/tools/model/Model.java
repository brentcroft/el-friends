package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.Materializer;
import org.xml.sax.InputSource;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface Model extends Map< String, Object >
{

    Map<String, Object> newContainer();

    Expander getExpander();
    Evaluator getEvaluator();

    default void logStep( String text ) {
        System.out.println( text );
    }

    Object putStatic( String key, Object value);
    Map<String,Object> getStaticModel();

    /**
     * Expands a value using the expander
     * or else just returns the value.
     *
     * @param value the value to be expanded
     * @return the expanded value
     */
    default String expand( String value )
    {
        return Optional
                .ofNullable(getExpander())
                .map(exp -> exp.apply( value, newContainer() ) )
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
        return Optional
                .ofNullable( getEvaluator() )
                .map( evaluator -> {
                    Map<String, Object> bindings = newContainer();
                    Object[] lastResult = {null};
                    Model
                            .stepsStream( value )
//                            .map( this::expand )
                            .forEach( step -> lastResult[0] = getEvaluator().apply( step, bindings ) );
                    return lastResult[0];
                } )
                .orElse( null );
    }

    default void run() {
        new Steps(this).run();
    }

    static Stream<String> stepsStream(String value) {
        String uncommented = Stream
                .of(value.split( "\\s*[\\n\\r]+\\s*" ))
                .filter( v -> !v.isEmpty() && !v.startsWith( "#" ) )
                .map( String::trim )
                .collect( Collectors.joining(" "));
        return Stream
                .of(uncommented.split( "\\s*[;]+\\s*" ));
    }

    default void steps(String steps) {
        new Steps(this, steps).run();
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

    File getLocalFile(String filePath);

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
        Model item = newChild( this, jsonText );
        filteredPutAll( item );
        return this;
    }

    default Model appendFromXml( InputSource inputSource ) {
        Model item = newItem();
        item.setParent( this );
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
        throw new IllegalArgumentException( format( "Object at path '%s' is not a Model: '%s'",
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
     *
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
                .map( p -> ( nonNull( p.getParent() ) ? format("%s.%s", p.path(), getName()) : getName())  )
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

    default Model whileDo( String booleanTest, String operation, int maxTries ) {
        int tries = 0;
        Supplier<Boolean> whileTest = () ->  {
            try {
                return (Boolean)eval( booleanTest );
            } catch (Exception e) {
                return false;
            }
        };
        while (whileTest.get() && tries < maxTries) {
            tries++;
            try {
                eval( operation );
                maybeDelay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if ( whileTest.get() ) {
            throw new IllegalArgumentException(format("Ran out of tries (%s) but: %s", tries, booleanTest ));
        }
        return this;
    }

    default Model whileDoAll( String booleanTest, List<String> operations, int maxTries ) {
        int tries = 0;
        Supplier<Boolean> whileTest = () ->  {
            try {
                return (Boolean)eval( booleanTest );
            } catch (Exception e) {
                return false;
            }
        };
        while (whileTest.get() && tries < maxTries) {
            tries++;
                operations.forEach( operation -> {
                    try {
                        eval( operation );
                        maybeDelay();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } );
        }
        if ( whileTest.get() ) {
            throw new IllegalArgumentException(format("Ran out of tries (%s) but: %s", tries, booleanTest ));
        }
        return this;
    }


    default void maybeDelay() {
        try
        {
            long delay = (long) getOrDefault( "$delay", 100L );
            Thread.sleep( delay );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
    interface Expander extends BiFunction<String, Map<String, Object>, String> {}
    interface Evaluator extends BiFunction<String, Map<String, Object>, Object> {}
    class Steps implements Runnable
    {
        private final String steps;
        private final Model model;
        private final boolean inline;

        private static final ThreadLocal< Stack<Model> > stack = ThreadLocal.withInitial( Stack::new );

        public Steps( Model model )
        {
            this.model = model;
            this.steps = Optional
                    .ofNullable(model.get("$steps"))
                    .map(Object::toString)
                    .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] has no value for $steps", model.path())));
            this.inline = false;
        }

        public Steps( Model model, String steps ) {
            this.model = model;
            this.steps = steps;
            this.inline = true;
        }

        public void run()
        {
            stack.get().push( model );
            try {
                String indent = IntStream
                        .range(0, stack.get().size() )
                        .mapToObj( i -> "  " )
                        .collect( Collectors.joining());

                String modelPath = model.path();

                model.logStep(
                        inline
                        ? format("%s%s(inline)", indent, modelPath.isEmpty() ? "" : (modelPath + ":"))
                        : format("%s%s$steps", indent, modelPath.isEmpty() ? "" : (modelPath + ".") )
                );

                Model
                        .stepsStream( model.expand( steps ) )
                        .peek( step -> model.logStep(format("%s -> %s", indent, step)) )
                        .forEach( model::eval );
            } finally {
                stack.get().pop();
            }
        }
    }
}
