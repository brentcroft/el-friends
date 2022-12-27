package com.brentcroft.tools.model;

import org.junit.Before;
import org.junit.Test;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import static org.junit.Assert.assertEquals;

public class ModelItemTest
{
    @Test
    public void createsModelItemFromJson() {
        Model item = new ModelItem().appendFromJson( "{ 'fred': 'bloggs' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }


    @Test
    public void assignsToSelf() {
        Model item = new ModelItem();
        item.eval( "$self" );
        assertEquals(item, item.eval( "$self" ));
    }
    @Test
    public void assignsToSelf02() {
        Model item = new ModelItem()
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .appendFromJson( "{ 'surname': 'bloggs' }" );
        item.eval( "$self.fred = surname" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void assignsToParent01() {
        Model item = new ModelItem().insertFromJson( "someChild", "{}" );
        assertEquals(item, item.getItem( "someChild" ).eval( "$parent" ));
    }

    @Test
    public void assignsToParent02() {
        Model item = new ModelItem()
                .appendFromJson( "{ 'fred': 'bloggs', 'xyz': { 'surname': 'bloggs' } }" );
        item.getItem( "xyz" ).eval( "$parent.blue = 'green'" );
        assertEquals("green", item.get( "blue" ));
    }

    @Test
    public void createsModelItemFromJsonFile() {
        Model item = new ModelItem().appendFromJson( "{ '$json': 'src/test/resources/root-01.json' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void loadsNestedFiles() {
        Model item = new ModelItem().appendFromJson( "{ '$json': 'src/test/resources/nested-01.json' }" );
        assertEquals(3, item.get( "level" ));
        assertEquals("3", item.expand( "${level}" ));
        assertEquals(3, item.eval( "level" ));

        assertEquals("plastic", item.eval( "days.wednesday.rubbish[2]" ));
        assertEquals(105, item.eval( "days.wednesday.rubbish[4]" ));
        assertEquals(105, item.eval( "days.friday.getParent().wednesday.rubbish[4]" ));


        System.out.println(item.toJson());
    }

    @Test
    public void overwritesModelItemFromPropertiesFile() {
        Model item = new ModelItem().appendFromJson( "{ '$json': 'src/test/resources/sub01/root-02.json' }" );
        assertEquals("boot", item.getItem( "less" ).get("foot"));
    }

    @Test
    public void insertsModelItem() {
        Model item = new ModelItem().appendFromJson( "{ 'fred': 'bloggs' }" );
        item.insertFromJson( "inserted", "{ 'fred': { 'head': 'nose' } }" );
        assertEquals("nose", item.getItem( "inserted.fred" ).get("head"));
    }

    @Test
    public void usesExpander() {
        Model item = new ModelItem()
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .insertFromJson( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        assertEquals("red", item.getItem( "inserted.fred" ).get("head"));
        assertEquals("red", item.expand( "${ inserted.fred['head'] }" ));
        assertEquals("red", item.expand( "${ inserted.fred.head }" ));
    }

    @Test
    public void usesEvaluator() {
        Model item = new ModelItem()
                .appendFromJson( "{ 'fred': 'bloggs' }" )
                .insertFromJson( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        Model expected = item.getItem( "inserted.fred" );
        assertEquals(expected, item.eval( "inserted.fred" ) );
        assertEquals(expected, item.eval( "inserted['fred']" ) );
    }
}
