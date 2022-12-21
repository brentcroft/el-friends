package com.brentcroft.tools.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModelItemTest
{
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
    public void overwritesModelItemFromPropertiesFile() {
        ModelItem item = ModelItem.of( null, "{ '$json': 'src/test/resources/root-02.json' }" );
        assertEquals("boot", item.get( "foot" ));
    }
}
