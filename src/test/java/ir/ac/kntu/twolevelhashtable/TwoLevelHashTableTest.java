package ir.ac.kntu.twolevelhashtable.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TwoLevelHashTableTest {
    private TwoLevelHashTable<Integer, String> hashTable;

    @BeforeEach
    void setUp() {
        hashTable = new TwoLevelHashTable<>(key -> key, 10);
    }

    @Test
    void testInsertAndSearchSingleValue() {
        hashTable.insert(1, "one");
        LinkedList<String> result = hashTable.search(1);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("one", result.getFirst());
    }

    @Test
    void testInsertAndSearchMultipleValuesWithCollision() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");
        LinkedList<String> result1 = hashTable.search(1);
        LinkedList<String> result2 = hashTable.search(11);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("one", result1.getFirst());
        assertEquals("eleven", result2.getFirst());
    }

    @Test
    void testDeletePrimaryKey() {
        hashTable.insert(1, "one");
        assertTrue(hashTable.delete(1));
        assertNull(hashTable.search(1));
    }

    @Test
    void testDeleteFromSecondaryTable() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");
        hashTable.delete(11);
        assertNull(hashTable.search(11));
        assertNotNull(hashTable.search(1));
    }

    @Test
    void testDeleteNonExistentKey() {
        assertTrue(hashTable.delete(5));
    }

    @Test
    void testContainsKey() {
        hashTable.insert(2, "two");
        assertTrue(hashTable.containsKey(2));
        assertFalse(hashTable.containsKey(3));
    }

    @Test
    void testSearchNonExistentKey() {
        assertNull(hashTable.search(5));
    }

    @Test
    void testCollisionHandling() {
        hashTable.insert(2, "two");
        hashTable.insert(12, "twelve");
        assertEquals(0.5, hashTable.getCollisionRate(), 0.001);
    }

    @Test
    void testMultipleCollisions() {
        hashTable.insert(3, "three");
        hashTable.insert(13, "thirteen");
        hashTable.insert(23, "twenty-three");
        assertEquals(2.0 / 3.0, hashTable.getCollisionRate(), 0.001);
    }

    @Test
    void testGetPrimaryTable() {
        hashTable.insert(1, "one");
        hashTable.insert(2, "two");
        Map<Integer, String> primary = hashTable.getPrimaryTable();
        assertEquals(2, primary.size());
        assertEquals("one", primary.get(1));
        assertEquals("two", primary.get(2));
    }

    @Test
    void testGetSecondaryTable() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");
        hashTable.insert(21, "twenty-one");

        Map<Integer, LinkedList<String>> secondary = hashTable.getSecondaryTable();
        assertTrue(secondary.containsKey(11));
        assertEquals(2, secondary.get(11).size());
        assertTrue(secondary.get(11).contains("eleven"));
        assertTrue(secondary.get(11).contains("twenty-one"));
    }

    @Test
    void testClearTable() {
        hashTable.insert(5, "five");
        hashTable.insert(15, "fifteen");
        hashTable.clear();
        assertNull(hashTable.search(5));
        assertNull(hashTable.search(15));
        assertEquals(0, hashTable.getCollisionRate(), 0.001);
    }

    @Test
    void testCollisionCountAfterDeletion() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");
        hashTable.delete(11);
        assertEquals(0.5, hashTable.getCollisionRate(), 0.001);
    }

    @Test
    void testUpdateCollisions() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");
        hashTable.insert(21, "twenty-one");
        hashTable.delete(11);
        hashTable.delete(21);
        assertEquals(0.6666666666666666, hashTable.getCollisionRate(), 0.001);
    }

    @Test
    void testDisplayOutput() {
        hashTable.insert(1, "one");
        hashTable.insert(2, "two");
        assertDoesNotThrow(() -> hashTable.display());
    }

    @Test
    void testSetNewHashFunction() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");

        hashTable.setHashCipher(key -> key % 5);
        assertNotNull(hashTable.search(1));
        assertNotNull(hashTable.search(11));
    }

    @Test
    void testToStringMethod() {
        hashTable.insert(1, "one");
        String output = hashTable.toString();
        assertTrue(output.contains("one"));
    }

    @Test
    void testInsertLargeNumberOfElements() {
        for (int i = 0; i < 100; i++) {
            hashTable.insert(i, "value" + i);
        }
        assertEquals(20, hashTable.getPrimaryTable().size() + hashTable.getSecondaryTable().size());
    }

    @Test
    void testRehashingDoesNotLoseData() {
        hashTable.insert(1, "one");
        hashTable.insert(11, "eleven");

        Function<Integer, Integer> newHashFunction = key -> (key * 2) % 10;
        hashTable.setHashCipher(newHashFunction);

        assertNotNull(hashTable.search(1));
        assertNotNull(hashTable.search(11));
    }
}
