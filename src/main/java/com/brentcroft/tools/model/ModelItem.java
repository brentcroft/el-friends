package com.brentcroft.tools.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

@Getter
@Setter
public class ModelItem extends LinkedHashMap<String,Object>
{
    private static JsonMapper jsonMapper = JsonMapper
            .builder()
            .enable( JsonReadFeature.ALLOW_JAVA_COMMENTS )
            .enable( JsonReadFeature.ALLOW_SINGLE_QUOTES )
            .enable( JsonReadFeature.ALLOW_YAML_COMMENTS )
            .enable( JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES )
            .enable( JsonReadFeature.ALLOW_TRAILING_COMMA )
            .build();

    private static BiFunction<String, ModelItem, String> valueTransformer = (value, context) -> value;

    public static void setValueTransformer(BiFunction<String, ModelItem, String> valueTransformer) {
        ModelItem.valueTransformer = valueTransformer;
    }
    public static BiFunction<String, ModelItem, String> getValueTransformer() {
        return ModelItem.valueTransformer;
    }
    public static void setJsonMapper(JsonMapper jsonMapper) {
        ModelItem.jsonMapper = jsonMapper;
    }
    public static JsonMapper getJsonMapper() {
        return ModelItem.jsonMapper;
    }

    private String name;
    private Map<String, Object> parent;

    public ModelItem getRoot() {
        return Optional
                .ofNullable( parent )
                .filter( p -> p instanceof ModelItem )
                .map( p -> (ModelItem)p )
                .map( ModelItem::getRoot )
                .orElse( this );
    }
    public ModelItem getItem(String key) {
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
        if (node instanceof ModelItem)
        {
            return (ModelItem)node;
        }
        throw new IllegalArgumentException(format("Object at path '%s' is not a ModeItem: '%s'",
                key,
                Optional
                .ofNullable(node)
                .map( Object::getClass )
                .map( Class::getSimpleName )
                .orElse( null )));
    }


    public Object get(String key) {
        return Optional
                .ofNullable( super.get( key ) )
                .map( p -> p instanceof String
                    ? expand((String)p)
                    : p)
                .orElseGet( () -> Optional
                        .ofNullable( parent )
                        .map( p -> {
                            Object v = p.get(key);
                            return  v instanceof String
                                    ? expand((String)v)
                                    : v;
                        })
                        .orElse( null ));
    }

    public String path() {
        return Optional
                .ofNullable( parent )
                .filter( p -> p instanceof ModelItem )
                .map( p -> (ModelItem)p )
                .filter( p -> nonNull(p.getParent()))
                .map( p -> p.path() + "." + name)
                .orElse( name );
    }

    private File getCurrentDirectory()
    {
        return Optional
                .ofNullable( parent )
                .map( p -> p.get( "$json" ).toString() )
                .map( File::new )
                .map( File::getParentFile )
                .orElse( null );
    }

    public static ModelItem of(Map< String, Object> parent, String jsonText) {
        ModelItem item;
        try
        {
            item = jsonMapper.readValue( jsonText, ModelItem.class);
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException(format("JSON text failed to load: %s", jsonText), e);
        }
        item.setParent( parent );
        transformMapsToItems(item);
        return item;
    }

    private static ModelItem of( String key, Map< String, Object> parent, Map< String, Object> items )
    {
        ModelItem item = new ModelItem();
        item.setName( key );
        item.setParent( parent );
        item.putAll( items );
        transformMapsToItems(item);
        return item;
    }


    public ModelItem insertModelItem( String key, String jsonText) {
        ModelItem item = ModelItem.of(this, jsonText);
        item.setName( key );
        transformMapsToItems(item);
        put(key, item);
        return item;
    }

    private static void transformMapsToItems( Map<String,Object> item )
    {
        for (String key : item.keySet() ) {
            Object value = item.get(key);
            if (value instanceof Map){
                if (value instanceof ModelItem) {
                    ModelItem childItem = (ModelItem) value;
                    childItem.setName( key );
                    childItem.setParent( item );
                } else {
                    ModelItem childItem = ModelItem.of( key, item, (Map<String,Object>)value );
                    childItem.maybeReplacements();
                    item.put( key, childItem );
                }
            }
        }
        if ( item instanceof ModelItem)
        {
            ((ModelItem) item).maybeReplacements();
        }
    }

    private void maybeReplacements() {
        if (containsKey( "$json" )) {
            overwriteFromFile(get("$json").toString());
        }
        if (containsKey( "$properties" )) {
            overwritePropertiesFromFile(get("$properties").toString());
        }
    }

    public void overwritePropertiesFromFile( String propertiesFilePath )
    {
        File file = new File(propertiesFilePath);
        if (!file.exists()) {
            file = new File( getCurrentDirectory(), propertiesFilePath );
        }
        final Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream( file )) {
            p.load( fis );
        } catch (Exception e) {
            throw new IllegalArgumentException(format( "Properties file not found: %s",propertiesFilePath ), e);
        }
        Function<String,String> uptoAnyPlaceHolder = value -> {
                int phi = value.indexOf("{0}");
                if (phi > -1) {
                    return value.substring( 0, phi );
                }
                return value;
        };
        p.forEach( (k,v) -> {
            final String ref = k.toString().trim();
            final String value = uptoAnyPlaceHolder.apply(v.toString().trim());
            final String[] segs = ref.split( "\\s*\\.\\s*" );
            Map<String,Object> target = this;
            for (int i = 0, n = segs.length; i< n; i++) {
                final Object segValue = target.get( segs[i] );
                if (segValue instanceof Map) {
                    target = (Map<String,Object>)segValue;
                    continue;
                }
                final String key = Arrays.stream( segs, i, n )
                        .collect( Collectors.joining("."));

                target.put( key, value );
            }
        } );
    }

    public void overwriteFromFile( String jsonFilePath )
    {
        ModelItem item = ModelItem.of( this, readFileFully(jsonFilePath) );
        putAll( item
                .entrySet()
                .stream()
                .filter( entry -> ! entry.getKey().startsWith( "$json" ))
                .filter( entry -> ! entry.getKey().startsWith( "$properties" ))
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ));
    }

    public String readFileFully( String filePath )
    {
        File file = new File(filePath);
        if (!file.exists()) {
            file = new File( getCurrentDirectory(), filePath );
        }
        try
        {
            return String
                    .join( "\n", Files
                    .readAllLines( file.toPath() ) );
        }
        catch ( IOException e )
        {
           throw new IllegalArgumentException(format("Invalid file path: %s", filePath), e);
        }
    }

    public String expand( String p )
    {
        return valueTransformer.apply( p, this );
    }

    public String  toJson()
    {
        try
        {
            return jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString( this );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException(format("ModelItem at path: %s", path()), e);
        }
    }

    public String toString() {
        return path();
    }

    public ModelItem getSelf() {
        return this;
    }
}
