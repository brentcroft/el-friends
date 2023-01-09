package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.ValidationException;
import com.brentcroft.tools.materializer.core.OpenEvent;
import com.brentcroft.tools.materializer.core.Tag;
import com.brentcroft.tools.materializer.core.TriConsumer;
import com.brentcroft.tools.materializer.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Getter
public enum ModelRootTag implements FlatTag< Model >
{
    DOCUMENT_ELEMENT( "*",
            ( model, event ) -> event.asStringMap().forEach( (k,v) -> model.put( "$" + k.trim(), v.trim() ) ),
            ( model, text) -> model.introspectEntries(),
            ModelTag.MODEL,
            EntryTag.EL,
            EntryTag.JSON,
            EntryTag.ENTRY,
            EntryTag.TEXT,
            EntryTag.STEPS,
            EntryTag.DATE,
            EntryTag.DATETIME,
            EntryTag.DURATION,
            EntryTag.INTEGER,
            EntryTag.LONG,
            EntryTag.DOUBLE,
            EntryTag.BOOLEAN,
            EntryTag.BIG_DECIMAL,
            EntryTag.BIG_INTEGER
            ),
    DOCUMENT_ROOT( "", DOCUMENT_ELEMENT );

    private final String tag;
    private final boolean multiple = true;
    private final boolean choice = true;
    private final FlatOpener< Model, OpenEvent > opener;
    private final FlatCloser< Model, String > closer;
    private final Tag< ? super Model, ? >[] children;

    @SafeVarargs
    ModelRootTag( String tag, BiConsumer< Model, OpenEvent> opener, BiConsumer< Model, String> closer, Tag< ? super Model, ? >... children )
    {
        this.tag = tag;
        this.opener = Opener.flatOpener( opener );
        this.closer = Closer.flatCloser( closer );
        this.children = children;
    }
    @SafeVarargs
    ModelRootTag( String tag, Tag< ? super Model, ? >... children )
    {
        this(tag, null, null, children);
    }
}

@Getter
enum ModelTag implements StepTag< Model, Model >
{
    MODEL(
            "model",
            ( model, event ) -> event.asStringMap().forEach( (k,v) -> model.put( "$" + k.trim(), v.trim() ) ),
            ( model, text) -> model.introspectEntries()
    ) {
        // dynamic method allows self-reference
        public Tag< ? super Model, ? >[] getChildren()
        {
            return Tag.tags(
                    MODEL,
                    EntryTag.EL,
                    EntryTag.JSON,
                    EntryTag.ENTRY,
                    EntryTag.TEXT,
                    EntryTag.STEPS,
                    EntryTag.DATE,
                    EntryTag.DATETIME,
                    EntryTag.DURATION,
                    EntryTag.INTEGER,
                    EntryTag.LONG,
                    EntryTag.DOUBLE,
                    EntryTag.BOOLEAN,
                    EntryTag.BIG_DECIMAL,
                    EntryTag.BIG_INTEGER
            );
        }
    };

    private final boolean multiple = true;
    private final boolean choice = true;
    private final String tag;
    private final FlatOpener< Model, OpenEvent > opener;
    private final FlatCloser< Model, String > closer;

    ModelTag( String tag, BiConsumer< Model, OpenEvent> opener, BiConsumer< Model, String> closer )
    {
        this.tag = tag;
        this.opener = Opener.flatOpener( opener );
        this.closer = Closer.flatCloser( closer );
    }

    @Override
    public Model getItem( Model model, OpenEvent event )
    {
        Model child = model.newItem();
        child.setName( event.getAttribute( "key" ) );
        child.setParent( model );
        model.put( child.getName(), child );
        return child;
    }
}

@Getter
enum EntryTag implements FlatTag< Model >
{
    EL(
            "el",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, model.eval( text.trim() )  ) ),

    JSON(
            "json",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> {
                try
                {
                    Object value = AbstractModelItem.JSON_MAPPER.readValue( text, Object.class );
                    model.put( key, value );
                }
                catch ( JsonProcessingException e )
                {
                    throw new ValidationException( EntryTag.valueOf( "JSON" ), e );
                }
            } ),

    ENTRY(
            "entry",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, text.trim() ) ),

    STEPS(
            "steps",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Model
                    .stepsStream( text.trim() )
                    .collect( Collectors.joining(" ; "))  ) ),

    TEXT(
            "text",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, text.trim() ) ),

    BOOLEAN(
            "boolean",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Boolean.parseBoolean( text.trim() )  ) ),

    INTEGER(
            "integer",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Integer.parseInt( text.trim() )  ) ),
    LONG(
            "long",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Long.parseLong( text.trim() )  ) ),
    DOUBLE(
            "double",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Double.parseDouble( text.trim() )  ) ),
    BIG_INTEGER(
            "big-integer",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, new BigInteger( text.trim() )  ) ),
    BIG_DECIMAL(
            "big-decimal",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, new BigDecimal( text.trim() )  ) ),
    DATE(
            "date",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, LocalDate.parse( text.trim() )  ) ),
    DATETIME(
            "datetime",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, LocalDateTime.parse( text.trim() )  ) ),
    DURATION(
            "duration",
            ( model, event ) -> event.getAttribute( "key" ),
            ( model, text, key ) -> model.put( key, Duration.parse( text.trim() )  ) )
    ;

    private final String tag;
    private final boolean multiple = false;
    private final boolean choice = false;
    private final FlatCacheOpener< Model, OpenEvent, ? > opener;
    private final FlatCacheCloser< Model, String, ? > closer;
    private final Tag< ? super Model, ? >[] children = null;

    < C > EntryTag(
            String tag,
            BiFunction< Model, OpenEvent, C > opener,
            TriConsumer< Model, String, C > closer
    )
    {
        this.tag = tag;
        this.opener = Opener.flatCacheOpener( opener );
        this.closer = Closer.flatCacheCloser( closer );
    }
}