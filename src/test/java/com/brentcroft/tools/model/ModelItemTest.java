package com.brentcroft.tools.model;

import org.junit.Before;
import org.junit.Test;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import static org.junit.Assert.assertEquals;

public class ModelItemTest
{
    @Before
    public void configureJstl() {
        JstlTemplateManager jstl = new JstlTemplateManager();
        ModelItem.setExpander( jstl::expandText );
        ModelItem.setEvaluator( jstl::eval );
    }

    @Test
    public void createsModelItemFromJson() {
        ModelItem item = ModelItem.of( null, "{ 'fred': 'bloggs' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void createsModelItemFromJsonFile() {
        ModelItem item = ModelItem.of( null,"{ '$json': 'src/test/resources/root-01.json' }" );
        assertEquals("bloggs", item.get( "fred" ));
    }

    @Test
    public void loadsNestedFiles() {
        ModelItem item = ModelItem.of( null,"{ '$json': 'src/test/resources/nested-01.json' }" );
        assertEquals(3, item.get( "level" ));
        assertEquals("3", item.expand( "${level}" ));
        assertEquals(3, item.eval( "level" ));

        assertEquals("plastic", item.eval( "days.wednesday.rubbish[2]" ));
        assertEquals(105, item.eval( "days.wednesday.rubbish[4]" ));

        System.out.println(item.toJson());
    }

    @Test
    public void overwritesModelItemFromPropertiesFile() {
        ModelItem item = ModelItem.of( null, "{ '$json': 'src/test/resources/root-02.json' }" );
        assertEquals("boot", item.get( "foot" ));
    }

    @Test
    public void insertsModelItem() {
        ModelItem item = ModelItem.of( null, "{ 'fred': 'bloggs' }" );
        item.insertModelItem( "inserted", "{ 'fred': { 'head': 'nose' } }" );
        assertEquals("nose", item.getItem( "inserted.fred" ).get("head"));
    }

    @Test
    public void usesExpander() {
        ModelItem item = ModelItem.of( null, "{ 'fred': 'bloggs' }" );
        item.insertModelItem( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        assertEquals("red", item.getItem( "inserted.fred" ).get("head"));
        assertEquals("red", item.expand( "${ inserted.fred['head'] }" ));
        assertEquals("red", item.expand( "${ inserted.fred.head }" ));
    }

    @Test
    public void usesEvaluator() {
        ModelItem item = ModelItem.of( null, "{ 'fred': 'bloggs' }" );
        item.insertModelItem( "inserted", "{ 'fred': { 'head': '${ hair }', 'hair': 'red' } }" );

        ModelItem expected = item.getItem( "inserted.fred" );
        assertEquals(expected, item.eval( "inserted.fred" ) );
        assertEquals(expected, item.eval( "inserted['fred']" ) );
    }
}
