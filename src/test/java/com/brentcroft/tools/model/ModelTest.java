package com.brentcroft.tools.model;

import com.brentcroft.tools.el.SimpleELContextFactory;
import com.brentcroft.tools.materializer.TagValidationException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class ModelTest
{
    private final ModelItem item = new ModelItem();

    @Before
    public void setCurrentDirectory() {
        item.setCurrentDirectory( Paths.get( "src/test/resources/models" ) );
        SimpleELContextFactory.clean();
    }

    @Test
    public void test_assignment()
    {
        item.appendFromJson( "{ 'colors': { 'time': 10 } }" );
        assertEquals( 10, item.eval( "colors.time") );

        item.eval( "$self.whileDo( () -> colors.time < 20, ( i ) -> (colors.time = colors.time + i ), 12 )" );
        assertEquals( 20L, item.eval( "colors.time" ) );

        // TODO: Figure out what's being incremented and why it's not colors.time
        item.eval( "colors.whileDo( () -> time < 30, () -> ($self.time = time + 1 ), 12 )" );
        //assertEquals( 30L, item.eval( "colors.time" ) );


        item.eval( "$self.whileDo( () -> true, () -> c:delay(500), 1, ( s ) -> (colors.timeWaiting = s ) )" );

        Double timeWaiting = ( Double ) item.eval( "colors.timeWaiting" );

        assertTrue(
                format( "Time waiting is too large: %s", timeWaiting ),
                Math.abs( 0.5 - timeWaiting ) < 0.1
        );
    }


    @Test
    public void loadsModel() {
        item.appendFromJson( "{ '$xml': 'model-01.xml' }" );
        assertEquals( "model-01", item.eval("$title") );
    }

    @Test
    public void testModelLambdas() {
        item.appendFromJson( "{ '$xml': 'model-01.xml' }" );
        item.eval("$self.testLambdaArgs()" );
    }

    @Test
    public void testStaticLambdas() {
        item.appendFromJson( "{ '$xml': 'model-02.xml' }" );
        item.eval("$self.testStaticLambdas()" );
    }

    @Test
    public void testHierarchy() {
        item.appendFromJson( "{ '$xml': 'model-03-hierarchy.xml' }" );
        item.eval("$self.testHierarchy01()" );
        item.eval("$self.testHierarchy02()" );
        item.eval("$self.testHierarchy03()" );
    }

    @Test
    public void testHierarchyConditionals() {
        item.appendFromJson( "{ '$xml': 'model-03-hierarchy.xml' }" );
        item.eval("$self.testHierarchyConditionals()" );
    }

    @Test
    public void localModelStaticScopes()
    {
        item.eval( "$static.vegetable = 'cabbage'" );
        assertEquals( "cabbage", item.eval( "vegetable" ) );

        item.eval( "$static.vegetable = 'cabbage'; $self.vegetable = 'turnip'" );
        assertEquals( "turnip", item.eval( "vegetable" ) );

        assertEquals( "cabbage", new ModelItem().eval( "vegetable" ) );

        Object result = item.eval( "$local.vegetable = 'chard'; $self.vegetable = 'cabbage'; vegetable" );
        assertEquals( "cabbage", result );
        assertEquals( "cabbage", item.eval( "vegetable" ) );
        assertEquals( "chard", new ModelItem().eval( "vegetable" ) );
    }
}
