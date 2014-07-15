package com.mellanox.jxio;

import java.util.HashMap;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.facet.taxonomy.LRUHashMap;

public class WorkerCache {
	public static final String                  CACHE_TAG    = "cacheId";
	private final int                           MAX_ENTRY_WORKERS  = 5;
	private final int                           MAX_HASH     = 1000;
	private final static Log                    LOG          = LogFactory.getLog(WorkerCache.class.getCanonicalName());
	private HashMap<String, LinkedList<Worker>> workersCache = new LRUHashMap<String, LinkedList<Worker>>(MAX_HASH);
	private WorkerProvider                      wp;

	public interface WorkerProvider {
		public Worker getWorker();
	}

	public interface Worker {
		public boolean isFree();
	}

	public WorkerCache(WorkerProvider wp) {
		this.wp = wp;
	}

	Worker getCachedWorker(String clientId) {
		Worker w;
		LinkedList<Worker> clientWorkers = workersCache.get(clientId);
		// first time this client connects or wasn't connected for a long time and was removed by LRU
		if (clientWorkers == null) {
			if (LOG.isDebugEnabled())
				LOG.debug("client id " + clientId + " wasn't found in hash");
			w = wp.getWorker();
			if (w != null) {
				LinkedList<Worker> list = new LinkedList<Worker>();
				list.add(w);
				workersCache.put(clientId, list);
			}
			return w;
		}
		// look for free worker in client's previously connected workers
		int pos = 0;
		while (pos < clientWorkers.size()) {
			w = clientWorkers.get(pos);
			if (w.isFree()) {
				if (LOG.isDebugEnabled())
					LOG.debug("found free worker" + w + " for client id " + clientId);
				clientWorkers.remove(pos);
				clientWorkers.addFirst(w);
				return w;
			}
			pos++;
		}
		// no free workers to use from previously connected, get a new one
		w = wp.getWorker();
		if (w != null) {
			clientWorkers.addFirst(w);
			if (clientWorkers.size() > MAX_ENTRY_WORKERS) {
				clientWorkers.removeLast();
			}
		}
		return w;
	}
}
