package com.brentcroft.tools.model;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

public class InteractiveFrameTest
{
    private final Model item = new ModelItem();

    @Before
    public void setCurrentDirectory() {
        item.setCurrentDirectory( Paths.get( "src/test/resources" ) );
        item.getStaticModel().clear();
        item.appendFromJson( "{ '$xml': 'brentcroft-site.xml' }" );
    }

    @Test
    @Ignore
    public void opensInteractiveFrame() {
        ModelInspectorDialog iframe = new ModelInspectorDialog( item );
        iframe.setSteps( "c:delay( 2000 ); c:println( 'hello world' )" );
        iframe.setModal( true );
        iframe.setVisible( true );

        item.getItem( "totals" ).call( "$$doubleJsonData" );

    }
}
