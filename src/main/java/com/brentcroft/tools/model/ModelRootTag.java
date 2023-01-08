package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.core.OpenEvent;
import com.brentcroft.tools.materializer.core.Tag;
import com.brentcroft.tools.materializer.core.TriConsumer;
import com.brentcroft.tools.materializer.model.*;
import lombok.Getter;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Getter
public enum ModelRootTag implements FlatTag< Model >
{
    DOCUMENT_ELEMENT( "*",
            ( model, event ) -> event.asMap().forEach( (k,v) -> model.put( "$" + k, v ) ),
            ( model, text) -> model.introspectEntries(),
            ModelTag.MODEL, EntryTag.ENTRY ),
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
            ( model, event ) -> event.asMap().forEach( (k,v) -> model.put( "$" + k, v ) ),
            ( model, text) -> model.introspectEntries()
    ) {
        // dynamic method allows self-reference
        public Tag< ? super Model, ? >[] getChildren()
        {
            return Tag.tags( MODEL, EntryTag.ENTRY );
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
    ENTRY(
            "entry",
            // cache the key
            ( model, event ) -> event.getAttribute( "key" ),
            // maybe do conversions??
            ( model, text, key ) -> model.put( key, text ) ),
    INTEGER(
            "entry",
                    // cache the key
                    ( model, event ) -> event.getAttribute( "key" ),
                // maybe do conversions??
                ( model, text, key ) -> model.put( key, text ) )
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