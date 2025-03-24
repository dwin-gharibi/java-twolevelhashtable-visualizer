package ir.ac.kntu.twolevelhashtable.table;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

public class TwoLevelHashTable<K, V> {
    private Object[] primaryTable;
    private Object[] secondaryTable;
    private Function<K, Integer> hashCipher;
    private int collisions;
    private int totalInserts;
    private int capacity;

    public TwoLevelHashTable(Function<K, Integer> hashCipher, int capacity) {
        this.hashCipher = hashCipher;
        this.capacity = capacity;
        this.primaryTable = new Object[capacity];
        this.secondaryTable = new Object[capacity];
        this.collisions = 0;
        this.totalInserts = 0;
    }

    public void setHashCipher(Function<K, Integer> newCipher) {
        this.hashCipher = newCipher;
        Object[] oldPrimary = primaryTable.clone();
        Object[] oldSecondary = secondaryTable.clone();
        clear();

        for (int i = 0; i < capacity; i++) {
            if (oldPrimary[i] != null) {
                Pair<K, V> pair = (Pair<K, V>) oldPrimary[i];
                insert(pair.key, pair.value);
            }
            if (oldSecondary[i] != null) {
                LinkedList<Pair<K, V>> list = (LinkedList<Pair<K, V>>) oldSecondary[i];
                for (Pair<K, V> pair : list) {
                    insert(pair.key, pair.value);
                }
            }
        }
    }

    public Function<K, Integer> getHashCipher() {
        return hashCipher;
    }

    public int getCapacity() {
        return capacity;
    }

    public void insert(K key, V value) {
        totalInserts++;
        int index = hashCipher.apply(key) % capacity;

        if (primaryTable[index] == null) {
            primaryTable[index] = new Pair<>(key, value);
        } else {
            collisions++;
            if (secondaryTable[index] == null) {
                secondaryTable[index] = new LinkedList<Pair<K, V>>();
            }
            LinkedList<Pair<K, V>> list = (LinkedList<Pair<K, V>>) secondaryTable[index];
            list.add(new Pair<>(key, value));
        }
    }

    public boolean delete(K key) {
        int index = hashCipher.apply(key) % capacity;

        if (primaryTable[index] != null && ((Pair<K, V>) primaryTable[index]).key.equals(key)) {
            primaryTable[index] = null;
        } else {
            if (secondaryTable[index] != null) {
                LinkedList<Pair<K, V>> list = (LinkedList<Pair<K, V>>) secondaryTable[index];
                boolean removed = list.removeIf(pair -> pair.key.equals(key));

                if (removed && list.isEmpty()) {
                    secondaryTable[index] = null;
                    return true;
                }
                return false;
            }
        }

        updateCollisions();
        return true;
    }


    public LinkedList<V> search(K key) {
        int index = hashCipher.apply(key) % capacity;
        LinkedList<V> results = new LinkedList<>();

        if (primaryTable[index] != null && ((Pair<K, V>) primaryTable[index]).key.equals(key)) {
            results.add(((Pair<K, V>) primaryTable[index]).value);
        }

        if (secondaryTable[index] != null) {
            LinkedList<Pair<K, V>> list = (LinkedList<Pair<K, V>>) secondaryTable[index];
            for (Pair<K, V> pair : list) {
                if (pair.key.equals(key)) {
                    results.add(pair.value);
                }
            }
        }

        return results.isEmpty() ? null : results;
    }

    public boolean containsKey(K key) {
        return search(key) != null;
    }

    private void updateCollisions() {
        collisions = 0;
        for (int i = 0; i < capacity; i++) {
            if (primaryTable[i] != null && secondaryTable[i] != null) {
                collisions++;
            }
        }
    }

    public void display() {
        System.out.println("Primary Table:");
        for (int i = 0; i < capacity; i++) {
            System.out.println(i + ": " + primaryTable[i]);
        }
        System.out.println("Secondary Table:");
        for (int i = 0; i < capacity; i++) {
            System.out.print(i + ": ");
            if (secondaryTable[i] != null) {
                System.out.println(secondaryTable[i]);
            } else {
                System.out.println("null");
            }
        }
    }

    public void clear() {
        for (int i = 0; i < capacity; i++) {
            primaryTable[i] = null;
            secondaryTable[i] = null;
        }
        collisions = 0;
        totalInserts = 0;
    }

    public double getCollisionRate() {
        return totalInserts == 0 ? 0 : (double) collisions / totalInserts;
    }

    public Map<K, V> getPrimaryTable() {
        Map<K, V> primaryMap = new HashMap<>();
        for (int i = 0; i < capacity; i++) {
            if (primaryTable[i] != null) {
                Pair<K, V> pair = (Pair<K, V>) primaryTable[i];
                primaryMap.put(pair.key, pair.value);
            }
        }
        return primaryMap;
    }

    public Map<K, LinkedList<V>> getSecondaryTable() {
        Map<K, LinkedList<V>> secondaryMap = new HashMap<>();
        for (int i = 0; i < capacity; i++) {
            if (secondaryTable[i] != null) {
                LinkedList<Pair<K, V>> list = (LinkedList<Pair<K, V>>) secondaryTable[i];
                LinkedList<V> values = new LinkedList<>();
                for (Pair<K, V> pair : list) {
                    values.add(pair.value);
                }
                secondaryMap.put(list.getFirst().key, values);
            }
        }
        return secondaryMap;
    }

    @Override
    public String toString() {
        return "Primary Table: " + java.util.Arrays.toString(primaryTable) + "\nSecondary Table: " + java.util.Arrays.toString(secondaryTable);
    }

    private static class Pair<K, V> {
        K key;
        V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }
    }
}
