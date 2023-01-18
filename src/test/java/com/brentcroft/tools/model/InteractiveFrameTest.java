package com.brentcroft.tools.model;

import org.junit.Before;
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
    public void opensInteractiveFrame() {
        ModelInspectorDialog iframe = new ModelInspectorDialog( item );
        iframe.setSteps( "c:println('hello world')" );
        iframe.setModal( true );
        iframe.setVisible( true );
    }
}
