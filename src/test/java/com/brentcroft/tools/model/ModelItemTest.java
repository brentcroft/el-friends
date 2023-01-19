package com.brentcroft.tools.model;

import com.brentcroft.tools.materializer.TagValidationException;
import org.junit.Before;
import org.junit.Ignore;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ModelItemTest
{
    final Model item = new ModelItem();

    @Before
    public void setCurrentDirectory() {
        item.setCurrentDirectory( Paths.get( "src/test/resources" ) );
        item.getStaticModel().clear();
    }

    @Test
    public void createsEmptyModelItemFromJson() {
        Model emptyModel = new ModelItem().appendFromJson( "{}" );
        assertEquals("{}", emptyModel.toJson().replaceAll( " ", "" ));
    }

    @Test
    public void createsModelItemFromJson() {
        item.appendFromJson( "{ 'fred': 'bloggs' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void calculatesPath() {
        item.appendFromJson( "{ 'people': { 'red': { 'hue': 123456 }, 'green': { 'hue': { 'x': 777 } }, 'blue': { 'hue': 345612 } } }" );
        assertEquals("people.green.hue", item.getItem( "people.green.hue" ).path());
        item.setName("root");
        assertEquals("people.green.hue", item.getItem( "people.green.hue" ).path());
    }

    @Test
    public void evaluatesRoot() {
        item
                .appendFromJson( "{ 'people': { 'red': { 'hue': 123456 }, 'green': { 'hue': { 'x': 777 } }, 'blue': { 'hue': 345612 } } }" );
        assertEquals(item, item.getItem( "people.green.hue" ).getRoot());
    }

    @Test
    public void evaluatesSelf() {
        assertEquals(item, item.eval( "$self" ));
    }
    @Test
    public void assignsToSelf() {
        item
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .appendFromJson( "{ 'surname': 'bloggs' }" );
        item.eval( "$self.fred = surname" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void evaluatesParent() {
        item.insertFromJson( "someChild", "{}" );
        assertEquals(item, item.getItem( "someChild" ).eval( "$parent" ));
    }

    @Test
    public void assignsToParent() {
        item
                .appendFromJson( "{ 'fred': 'bloggs', 'xyz': { 'surname': 'bloggs' } }" );
        item.getItem( "xyz" ).eval( "$parent.blue = 'green'" );
        assertEquals("green", item.get( "blue" ));
    }

    @Test
    public void createsModelItemFromJsonFile() {
        item.appendFromJson( "{ '$json': 'root-01.json' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void loadsNestedFiles() {
        item.appendFromJson( "{ '$json': 'nested-01.json' }" );
        assertEquals(3, item.get( "level" ));
        assertEquals("3", item.expand( "${level}" ));
        assertEquals(3, item.eval( "level" ));

        assertEquals("plastic", item.eval( "days.wednesday.rubbish[2]" ));
        assertEquals(105, item.eval( "days.wednesday.rubbish[4]" ));
        assertEquals(105, item.eval( "days.friday.getParent().wednesday.rubbish[4]" ));
    }

    @Test
    public void overwritesModelItemFromPropertiesFile() {
        item.appendFromJson( "{ '$json': 'sub01/root-02.json' }" );
        assertEquals("boot", item.getItem( "less" ).get("foot"));
    }

    @Test
    public void overwritesModelItemFromPropertiesXmlFile() {
        item.appendFromJson( "{ '$properties-xml': 'properties.xml' }" );
        assertEquals("234", item.get( "amount" ));
    }


    @Test
    public void materializesModelItemFromXmlFileReference()
    {
        item.appendFromJson( "{ '$xml': 'brentcroft-site.xml' }" );

        System.out.println(item.toJson());

        assertEquals(234.0, item.get( "amount" ));
        assertEquals(0.15, item.eval( "totals.amount" ));
        assertTrue((Boolean)item.eval( "totals.valid" ));
        assertEquals( Duration.parse( "P2DT3H4M" ), item.eval( "totals.timeTaken" ));
        assertEquals( LocalDateTime.parse( "2007-12-03T10:15:30.123456789" ), item.eval( "totals.timestamp" ));

        List<?> data = (List<?>)item.eval( "totals.data" );
        long[] counter = {1};
        data.forEach( datum -> assertEquals( counter[0]++, datum ) );

        List<?> jsonData = (List<?>)item.eval( "totals.jsonData" );
        int[] counter2 = {1};
        jsonData.forEach( datum -> assertEquals( counter2[0]++, datum ) );

        assertEquals("RED", item.eval( "angular.$shadow.color" ));
    }

    @Test
    public void appliesXmlOnload()
    {
        item.appendFromJson( "{ '$xml': 'brentcroft-site-onload.xml' }" );
        assertEquals("stone", item.eval( "home.blarney" ));
    }

    @Test
    public void appliesJsonOnload()
    {
        item.appendFromJson( "{ '$$run': '$self.fred = \"bloggs\"', '$onload': '$self.run()' }" );
        assertEquals("bloggs", item.eval( "fred" ));
    }


    @Test
    public void materializesModelItem() throws FileNotFoundException
    {
        Path path = Paths.get( item.getCurrentDirectory().toString(), "brentcroft-site.xml");
        item.appendFromXml( new InputSource( new FileInputStream( path.toFile() ) ) );

        assertEquals(234.0, item.get( "amount" ));
        assertEquals(0.15, item.getItem( "totals" ).get( "amount" ));
    }

    @Test
    public void insertsModelItem() {
        item.appendFromJson( "{ 'fred': 'bloggs' }" );
        item.insertFromJson( "inserted", "{ 'fred': { 'head': 'nose' } }" );
        assertEquals("nose", item.getItem( "inserted.fred" ).get("head"));
    }

    @Test
    public void usesExpander() {
        item
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .insertFromJson( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        assertEquals("red", item.getItem( "inserted.fred" ).get("head"));
        assertEquals("red", item.expand( "${ inserted.fred['head'] }" ));
        assertEquals("red", item.expand( "${ inserted.fred.head }" ));
    }

    @Test
    public void usesEvaluator() {
        item
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .insertFromJson( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        Model expected = item.getItem( "inserted.fred" );
        assertEquals(expected, item.eval( "inserted.fred" ) );
        assertEquals(expected, item.eval( "inserted['fred']" ) );
    }


    @Test
    public void usesWhileDo() {
        item
                .appendFromJson( "{ digits: [ 'a', 'b', 'c', '3', '5', '6', '7', '8', '9' ] }" );

        Object actual = item
                .whileDo( "digits.size() > 0", "digits.remove( digits[0] )", 12 )
                .eval( "digits" );

        assertEquals( Collections.emptyList(), actual );
    }


    @Test
    public void usesWhileDoAll() {
        item
                .appendFromJson( "{ digits: [ 'a', 'b', 'c', '3', '5', '6', '7', '8', '9' ] }" );

        Object actual = item
                .whileDoAll(
                        "digits.size() > 0",
                        Stream
                                .of(
                                        "digits.remove( digits[0] )",
                                        "c:println( digits[0] )" )
                                .collect( Collectors.toList()),
                        12 )
                .eval( "digits" );

        assertEquals( Collections.emptyList(), actual );
    }

    @Test
    public void usesWhileDoAllInSteps() {
        item
                .appendFromJson( "{ digits: [ 'a', 'b', 'c', '3', '5', '6', '7', '8', '9' ] }" );
        item
                .steps( "$self.whileDoAll( 'digits.size() > 0', [ 'digits.remove( digits[0] )', 'c:println( digits[0] )' ], 12)" );
        Object actual = item.eval( "digits" );
        assertEquals( Collections.emptyList(), actual );
    }

    @Test
    public void usesModelSteps() {
        item
                .appendFromJson( "{ '$json': 'nested-01.json' }" )
                .insertFromJson( "incrementer","{ '$$run': '$parent.level = level + 1' }" );

        item.setName( "root" );

        assertEquals(3, item.get( "level" ));

        item.getItem( "incrementer" ).run();

        assertEquals(4L, item.get( "level" ));
    }

    @Test
    public void usesModelStepsInline() {
        item
                .appendFromJson( "{ '$json': 'nested-01.json' }" )
                .insertFromJson( "incrementer","{ '$$run': '$parent.level = level + 1' }" );

        item.setName( "root" );
        assertEquals(3, item.get( "level" ));

        item.steps( "$self.level = level + 1; $self.level = level + 1; $self.level = level + 1; " );
        assertEquals(6L, item.get( "level" ));

        item.getItem( "incrementer" ).run();
        assertEquals(7L, item.get( "level" ));

        item.steps( "incrementer.run(); incrementer.run(); incrementer.run(); " );
        assertEquals(10L, item.get( "level" ));
    }

    @Test
    public void usesStaticScope() {
        Object oldValue = item.putStatic( "vegetable", "carrot" );
        assertNull(oldValue);
        assertEquals("carrot", item.eval( "vegetable" ));

        item.eval( "$self.vegetable = 'potato'" );
        assertEquals("potato", item.eval( "vegetable" ));

        assertEquals("carrot", new ModelItem().eval( "vegetable" ));
    }

    @Test
    public void usesStaticScopeInSteps() {
        item.steps( "$static.vegetable = 'cabbage'" );
        assertEquals("cabbage", item.eval( "vegetable" ));

        item.eval( "$self.vegetable = 'potato'" );
        assertEquals("potato", item.eval( "vegetable" ));

        assertEquals("cabbage", new ModelItem().eval( "vegetable" ));
    }

    @Test
    public void usesLocalScopeInSteps() {
        item.steps( "$static.vegetable = 'cabbage'; $local.vegetable = 'chard'" );
        assertEquals("cabbage", item.eval( "vegetable" ));

        item.steps( "$local.vegetable = 'chard'; $self.vegetable = $local.vegetable" );
        assertEquals("chard", item.eval( "vegetable" ));

        assertEquals("cabbage", new ModelItem().eval( "vegetable" ));
    }

    @Test(expected = TagValidationException.class)
    public void circularityXml()
    {
        item.appendFromJson( "{ '$xml': 'circularity.xml' }" );
    }

    @Test(expected = TagValidationException.class)
    public void circularityJsonXml()
    {
        item.appendFromJson( "{ '$json': 'circularity-xml.json' }" );
    }

    @Test(expected = CircularityException.class)
    public void circularityJson()
    {
        item.appendFromJson( "{ '$json': 'circularity.json' }" );
    }
    @Test(expected = TagValidationException.class)
    public void circularityXmlJson()
    {
        item.appendFromJson( "{ '$xml': 'circularity-json.xml' }" );
    }
}
