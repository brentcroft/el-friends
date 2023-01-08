package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.core.OpenEvent;
import com.brentcroft.tools.materializer.core.Tag;
import com.brentcroft.tools.materializer.core.TriConsumer;
import com.brentcroft.tools.materializer.model.*;
import lombok.Getter;

import java.util.Properties;
import java.util.function.BiFunction;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
public enum PropertiesRootTag implements FlatTag< Properties >
{
    ENTRY(
            "entry",
            ( properties, event ) -> event.getAttribute( "key" ),
            ( properties, text, cache ) -> properties.setProperty( cache, text ) ),
    COMMENT( "comment" ),
    PROPERTIES( "*", ENTRY, COMMENT ),
    ROOT( "", PROPERTIES );

    private final String tag;
    private final boolean multiple;
    private final boolean choice;
    private final FlatCacheOpener< Properties, OpenEvent, ? > opener;
    private final FlatCacheCloser< Properties, String, ? > closer;
    private final Tag< ? super Properties, ? >[] children;

    @SafeVarargs
    PropertiesRootTag( String tag, Tag< ? super Properties, ? >... children )
    {
        this( tag,null, null, children );
    }

    @SafeVarargs
    < C > PropertiesRootTag(
            String tag,
            BiFunction< Properties, OpenEvent, C > opener,
            TriConsumer< Properties, String, C > closer,
            Tag< ? super Properties, ? >... children
    )
    {
        this.tag = tag;
        this.multiple = isNull( children ) || children.length == 0;
        this.opener = Opener.flatCacheOpener( opener );
        this.closer = Closer.flatCacheCloser( closer );
        this.choice = nonNull( children ) && children.length > 0;
        this.children = children;
    }
}