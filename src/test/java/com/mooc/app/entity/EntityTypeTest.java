package com.mooc.app.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityTypeTest {

    @Test
    void entityType_hasPostAndSpot() {
        EntityType[] values = EntityType.values();
        assertEquals(2, values.length);
        assertEquals(EntityType.POST, EntityType.valueOf("POST"));
        assertEquals(EntityType.SPOT, EntityType.valueOf("SPOT"));
    }
}
