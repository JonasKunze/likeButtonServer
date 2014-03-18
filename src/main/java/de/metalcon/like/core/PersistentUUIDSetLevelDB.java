package de.metalcon.like.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Jonas Kunze
 */
public class PersistentUUIDSetLevelDB implements Iterable<Long> {
	private static LevelDBHandler db;

	private final long ID;

	public static void initialize() {
		db = new LevelDBHandler("PersistentUUIDSetLevelDB");
	}

	public PersistentUUIDSetLevelDB(final long ID) {
		this.ID = ID;
	}

	public PersistentUUIDSetLevelDB(final String ID) {
		this.ID = ID.hashCode();
	}

	/**
	 * Adds the given uuid to the end of the file
	 */
	public void add(long uuid) {
		db.setAdd(ID, uuid);
	}

	/**
	 * Deletes all entries from the Set
	 */
	public void delete() {
		// TODO To be implemented
	}

	/**
	 * This method is relatively expensive!
	 * 
	 * @param key
	 * @return
	 */
	public boolean contains(long key) {
		return db.setContainsElement(ID, key);
	}

	@Override
	public Iterator<Long> iterator() {
		return new ArrayIterator(db.getLongs(ID));
	}

	/**
	 * 
	 * @param uuid
	 *            Thee uuid to be removed
	 * @return Returns true if this set contained the uuid
	 */
	public boolean remove(long uuid) {
		/*
		 * FIXME: do we really need to return a boolean? contains(uuid) is quite
		 * expensive!
		 */
		boolean contains = contains(uuid);
		db.removeFromSet(ID, uuid);
		return contains;
	}

	public boolean remove(Node n) {
		return remove(n.getUUID());
	}

	/**
	 * Writes all uuids into the given array
	 * 
	 * @param array
	 * @return
	 */
	public long[] toArray() {
		return db.getLongs(ID);
	}

	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}
}

class ArrayIterator implements Iterator<Long> {
	private final long array[];
	private int pos = 0;

	public ArrayIterator(long anArray[]) {
		array = anArray;
	}

	@Override
	public boolean hasNext() {
		return pos < array.length;
	}

	@Override
	public Long next() throws NoSuchElementException {
		if (hasNext()) {
			return array[pos++];
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
