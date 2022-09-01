/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.cdo.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Implements a Least recently used cache with a specific size
 * @author werkmane
 *
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1362168190272073180L;
	private final int size;

	private LRUCache(int size) {
		super(size, 0.75f, true); //accessOrder = true
		this.size = size;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > size;
	}

	/**
	 * Creates a new Least Recently Used cache with a maximum size
	 * @param <K> key type
	 * @param <V> value type
	 * @param size the number of items in the map before they get evicted
	 * @return
	 */
	public static <K, V> LRUCache<K, V> newInstance(int size) {
		return new LRUCache<K, V>(size);
	}

	public static <K, V> Map<K, V> newSynchronizedInstance(int size) {
		LRUCache<K,V> lruCache = new LRUCache<K, V>(size);
		return Collections.synchronizedMap(lruCache);
	}

}
