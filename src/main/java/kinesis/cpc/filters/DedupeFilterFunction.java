package kinesis.cpc.filters;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * This class filters duplicates that occur within a configurable time of each other in a data stream.
 */
public class DedupeFilterFunction<T, K extends Serializable> extends RichFilterFunction<T> {

	private LoadingCache<K, Boolean> dedupeCache;
	private final KeySelector<T, K> keySelector;
	private final long cacheExpirationTimeMs;

	/**
	 * @param cacheExpirationTimeMs The expiration time for elements in the cache
	 */
	public DedupeFilterFunction(KeySelector<T, K> keySelector, long cacheExpirationTimeMs) {
		this.keySelector = keySelector;
		this.cacheExpirationTimeMs = cacheExpirationTimeMs;
	}

	@Override
	public void open(Configuration parameters) {
		createDedupeCache();
	}

	public boolean filter(T value) throws Exception {
		K key = keySelector.getKey(value);
		boolean seen = dedupeCache.get(key);
		if (!seen) {
			dedupeCache.put(key, true);
			return true;
		} else {
			return false;
		}
	}

	private void createDedupeCache() {
		dedupeCache = CacheBuilder.newBuilder()
				.expireAfterWrite(cacheExpirationTimeMs, TimeUnit.MILLISECONDS)
				.build(new CacheLoader<>() {
					@Override
					public Boolean load(K k) {
						return false;
					}
				});
	}
}
