package com.brentcroft.tools.model;

import com.brentcroft.tools.el.MapBindings;
import com.brentcroft.tools.materializer.Materializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

@Getter
@Setter
public abstract class AbstractModelItem extends LinkedHashMap< String, Object > implements Model
{
    public static String OPERATION_DELAY_MILLIS = "$operationDelayMillis";

    protected static Materializer< Properties > PROPERTIES_XML_MATERIALIZER = new Materializer<>(
            () -> PropertiesRootTag.ROOT,
            Properties::new );

    protected static JsonMapper JSON_MAPPER = JsonMapper
            .builder()
            .enable( JsonReadFeature.ALLOW_JAVA_COMMENTS )
            .enable( JsonReadFeature.ALLOW_SINGLE_QUOTES )
            .enable( JsonReadFeature.ALLOW_YAML_COMMENTS )
            .enable( JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES )
            .enable( JsonReadFeature.ALLOW_TRAILING_COMMA )
            .build();


    private static final ThreadLocal< Stack< Path > > pathStack = ThreadLocal.withInitial( Stack::new );
    public static Map< String, Object > staticModel;
    public static ThreadLocal< Stack< MapBindings > > scopeStack;


    static
    {
        JSON_MAPPER.registerModule( new JavaTimeModule() );
        JSON_MAPPER.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        JSON_MAPPER.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
    }

    public static String stringify( Object value )
    {
        try
        {
            return JSON_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString( value );
        }
        catch ( JsonProcessingException e )
        {
            throw new ModelException( format( "Bad stringification: %s", value ), e );
        }
    }

    protected static String readFileFully( File file )
    {
        try
        {
            return String
                    .join( "\n", Files
                            .readAllLines( file.toPath() ) );
        }
        catch ( IOException e )
        {
            throw new ModelException( format( "Invalid file: %s", file ), e );
        }
    }

    private String name = "?";
    private Map< String, Object > parent;


    public Object get( Object key )
    {
        if ( key == null )
        {
            return null;
        }
        return Optional
                .ofNullable( super.get( key ) )
                .map( p -> p instanceof String ? expand( ( String ) p ) : p )
                .orElse( null );
    }

    public Object put( String key, Object value )
    {
        if ( value instanceof Model )
        {
            ( ( Model ) value ).setParent( this );
        }
        return super.put( key, value );
    }

    /**
     * Bypasses checking for Models and calls super.put(key, value).
     */
    public Object set( String key, Object value )
    {
        return super.put( key, value );
    }

    public Path getCurrentDirectory()
    {
        return Optional
                .ofNullable( ( String ) get( "$currentDirectory" ) )
                .map( Paths::get )
                .orElseGet( () -> Optional
                        .ofNullable( getParent() )
                        .filter( p -> p instanceof Model )
                        .map( p -> ( ( Model ) p ).getCurrentDirectory() )
                        .orElse( Paths.get( "." ) ) );
    }

    public void setCurrentDirectory( Path directoryPath )
    {
        File directory = directoryPath.toFile();
        if ( ! directory.exists() )
        {
            throw new ModelException( format( "Directory does not exist: %s", directory.getPath() ) );
        }
        if ( ! directory.isDirectory() )
        {
            throw new ModelException( format( "Not a directory: %s", directory.getPath() ) );
        }

        File cd = getCurrentDirectory().toFile();
        if ( ! cd.equals( directory ) )
        {
            put( "$currentDirectory", directoryPath.toString() );
        }
    }

    public Model newItemFromJson( String jsonText )
    {
        try
        {
            return JSON_MAPPER.readValue( jsonText, getModelClass() );
        }
        catch ( JsonProcessingException e )
        {
            throw new ModelException( format( "JSON text failed to materialize: %s", jsonText ), e );
        }
    }

    public void introspectEntries()
    {
        if ( containsKey( "$json" ) )
        {
            Stream
                    .of( get( "$json" ).toString().split( "\\s*,\\s*" ) )
                    .map( String::trim )
                    .filter( filename -> ! filename.isEmpty() )
                    .forEach( filename -> {
                        File file = getLocalFile( filename );
                        putOnFileStack( file.toPath() );
                        try
                        {
                            setCurrentDirectory( file.getParentFile().toPath() );
                            appendFromJson( AbstractModelItem.readFileFully( file ) );
                        }
                        finally
                        {
                            pathStack.get().pop();
                        }
                    } );
        }
        if ( containsKey( "$xml" ) )
        {
            Object xmlValue = get( "$xml" );
            Stream
                    .of( xmlValue.toString().split( "\\s*,\\s*" ) )
                    .map( String::trim )
                    .filter( filename -> ! filename.isEmpty() )
                    .forEach( filename -> {
                        File file = getLocalFile( filename );
                        putOnFileStack( file.toPath() );
                        try
                        {
                            InputSource is = new InputSource( new FileInputStream( file ) );
                            is.setSystemId( file.getAbsolutePath() );
                            appendFromXml( is );
                        }
                        catch ( FileNotFoundException e )
                        {
                            throw new ModelException( format( "Bad $xml filename: %s", filename ), e );
                        }
                        finally
                        {
                            pathStack.get().pop();
                        }
                    } );
        }
        if ( containsKey( "$properties" ) )
        {
            Stream
                    .of( get( "$properties" ).toString().split( "\\s*,\\s*" ) )
                    .map( String::trim )
                    .filter( filename -> ! filename.isEmpty() )
                    .forEach( filename -> overwritePropertiesFromFile( filename, false ) );
        }
        if ( containsKey( "$properties-xml" ) )
        {
            Stream
                    .of( get( "$properties-xml" ).toString().split( "\\s*,\\s*" ) )
                    .map( String::trim )
                    .filter( filename -> ! filename.isEmpty() )
                    .forEach( filename -> overwritePropertiesFromFile( filename, true ) );
        }
        if ( containsKey( "$onload" ) )
        {
            eval( get( "$onload" ).toString() );
        }
    }

    protected void putOnFileStack( Path path )
    {
        if ( ! pathStack.get().isEmpty() && pathStack.get().stream()
                .anyMatch( p -> p.equals( path ) ) )
        {
            throw new CircularityException( format( "File: '%s' is already on the stack", path ) );
        }
        pathStack.get().push( path );
    }

    public File getLocalFile( String filePath )
    {
        return Optional
                .of( new File( filePath ) )
                .filter( File::exists )
                .orElseGet( () -> {
                    File cd = getCurrentDirectory().toFile();
                    return Optional
                            .of( new File( cd, filePath ) )
                            .filter( File::exists )
                            .orElseThrow( () -> new IllegalArgumentException( format( "Local file does not exist: %s/%s", cd, filePath ) ) );
                } );
    }

    public void putAll( Map< ? extends String, ? > item )
    {
        super.putAll( item
                .entrySet()
                .stream()
                .peek( entry -> {
                    if ( entry.getValue() instanceof Model )
                    {
                        ( ( Model ) entry.getValue() ).setParent( this );
                    }
                } )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
    }

    public void filteredPutAll( Map< ? extends String, ? > item )
    {
        putAll( item
                .entrySet()
                .stream()
                .filter( entry -> ! containsKey( entry.getKey() ) )
                .filter( entry -> ! entry.getKey().startsWith( "$onload" ) )
                .filter( entry -> ! entry.getKey().startsWith( "$json" ) )
                .filter( entry -> ! entry.getKey().startsWith( "$xml" ) )
                .filter( entry -> ! entry.getKey().startsWith( "$properties" ) )
                .filter( entry -> ! entry.getKey().startsWith( "$properties-xml" ) )
                .filter( entry -> ! entry.getKey().startsWith( "$currentDirectory" ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );
    }

    @SuppressWarnings( "unchecked" )
    private void overwritePropertiesFromFile( String propertiesFilePath, boolean isXml )
    {
        File file = new File( propertiesFilePath );
        if ( ! file.exists() )
        {
            file = new File( getCurrentDirectory().toFile(), propertiesFilePath );
        }

        Properties p;
        try ( FileInputStream fis = new FileInputStream( file ) )
        {
            if ( isXml )
            {
                p = PROPERTIES_XML_MATERIALIZER.apply( new InputSource( fis ) );
            }
            else
            {
                p = new Properties();
                p.load( fis );
            }
        }
        catch ( Exception e )
        {
            throw new ModelException( format( "Properties file not found: %s", file ), e );
        }
        Function< String, String > uptoAnyPlaceHolder = value -> {
            int phi = value.indexOf( "{0}" );
            if ( phi > - 1 )
            {
                return value.substring( 0, phi );
            }
            return value;
        };
        p.forEach( ( k, v ) -> {
            final String ref = k.toString().trim();
            final String value = uptoAnyPlaceHolder.apply( v.toString().trim() );
            final String[] segs = ref.split( "\\s*\\.\\s*" );
            Map< String, Object > target = this;
            for ( int i = 0, n = segs.length; i < n; i++ )
            {
                final Object segValue = target.get( segs[ i ] );
                if ( segValue instanceof Map )
                {
                    target = ( Map< String, Object > ) segValue;
                    continue;
                }
                final String key = Arrays
                        .stream( segs, i, n )
                        .collect( Collectors.joining( "." ) );

                target.put( key, value );
            }
        } );
    }

    @Override
    public String toJson()
    {
        try
        {
            return JSON_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString( this );
        }
        catch ( JsonProcessingException e )
        {
            throw new ModelException( format( "ModelItem at path: %s", path() ), e );
        }
    }

    @Override
    public String toString()
    {
        return path();
    }

    @Override
    public Model getSelf()
    {
        return this;
    }

    @Override
    public Object steps( String steps, Map< String, Object > argMap )
    {
        return new Steps( steps ).run( argMap );
    }

    public void maybeDelay()
    {
        try
        {
            long delay = ( long ) getOrDefault( OPERATION_DELAY_MILLIS, 100L );
            Thread.sleep( delay );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

    class Steps
    {
        private final String steps;

        public Steps( String steps )
        {
            this.steps = steps;
        }

        public Object run( Map< String, Object > argMap )
        {
            scopeStack.get().push( new MapBindings( argMap ) );

            try
            {
                String indent = IntStream
                        .range( 0, scopeStack.get().size() )
                        .mapToObj( i -> "  " )
                        .collect( Collectors.joining() );

                String modelPath = path();

                notifyModelEvent(
                        ModelEvent
                                .EventType
                                .STEPS_START
                                .newEvent(
                                        AbstractModelItem.this,
                                        format( "%s%s (inline)", indent, modelPath.isEmpty() ? "" : ( modelPath + ":" ) ) ) );

                Object[] lastResult = { null };

                Model
                        .stepsStream( steps )
                        .peek( step -> notifyModelEvent(
                                ModelEvent
                                        .EventType
                                        .STEP_START
                                        .newEvent(
                                                AbstractModelItem.this,
                                                format( "%s -> %s", indent, step ) ) ) )
                        .map( AbstractModelItem.this::expand )
                        .forEach( step -> lastResult[ 0 ] = eval( step ) );

                return lastResult[ 0 ];

            }
            catch ( RuntimeException e )
            {
                if ( e.getCause() instanceof Supplier )
                {
                    return ( ( Supplier< ? > ) e.getCause() ).get();
                }
                // reduce nested exceptions
                if ( e.getCause() != null && e.getClass().isAssignableFrom( e.getCause().getClass() ) )
                {
                    throw ( RuntimeException ) e.getCause();
                }
                throw e;
            }
            finally
            {
                scopeStack.get().pop();
            }
        }
    }
}
