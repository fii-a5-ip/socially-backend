package com.soccialy.backend.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupUserIdTest {

    @Test
    void equals_SameInstance_ReturnsTrue() {
        GroupUserId id1 = new GroupUserId(1, 2);
        assertEquals(id1, id1);
    }

    @Test
    void equals_NullObject_ReturnsFalse() {
        GroupUserId id1 = new GroupUserId(1, 2);

        assertNotEquals(null, id1);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        GroupUserId id1 = new GroupUserId(1, 2);
        Object otherObject = new Object();

        assertNotEquals(id1, otherObject);
    }

    @Test
    void equals_DifferentValues_ReturnsFalse() {
        GroupUserId id1 = new GroupUserId(1, 2);
        GroupUserId idDifferentGroup = new GroupUserId(99, 2);
        GroupUserId idDifferentUser = new GroupUserId(1, 99);
        GroupUserId idCompletelyDifferent = new GroupUserId(88, 99);

        assertNotEquals(id1, idDifferentGroup);
        assertNotEquals(id1, idDifferentUser);
        assertNotEquals(id1, idCompletelyDifferent);
    }

    @Test
    void equalsAndHashCode_SameValues_ReturnsTrueAndSameHash() {
        GroupUserId id1 = new GroupUserId(1, 2);
        GroupUserId id2 = new GroupUserId(1, 2);

        assertEquals(id1, id2);

        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void equalsAndHashCode_WithNullValues_HandledCorrectly() {
        GroupUserId idNull1 = new GroupUserId(null, null);
        GroupUserId idNull2 = new GroupUserId(null, null);
        GroupUserId idMixed = new GroupUserId(1, null);

        assertEquals(idNull1, idNull2);
        assertEquals(idNull1.hashCode(), idNull2.hashCode());

        assertNotEquals(idNull1, idMixed);
    }
}