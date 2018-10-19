/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.util;

import io.netty.buffer.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: guo
 * Date: 2017/11/29
 * Time: 上午9:45
 * Description:
 * 源至:https://github.com/isdom/jocean-http/blob/bb3c439e1af4be63688ddd1b53f8711493d2f23d/jocean-http/src/main/java/org/jocean/http/util/PooledAllocatorStats.java#L14
 */
public class PooledAllocatorStats {
    public Map<String, Object> getMetrics() {
        final PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        final Map<String, Object> metrics = new HashMap<>();
        {
            int idx = 0;
            for (PoolArenaMetric poolArenaMetric : allocator.directArenas()) {
                metrics.put("1_DirectArena[" + idx++ + "]", metricsOfPoolArena(poolArenaMetric));
            }
        }
        {
            int idx = 0;
            for (PoolArenaMetric poolArenaMetric : allocator.heapArenas()) {
                metrics.put("2_HeapArena[" + idx++ + "]", metricsOfPoolArena(poolArenaMetric));
            }
        }

        return metrics;
    }

    private static Map<String, Object> metricsOfPoolArena(
            final PoolArenaMetric poolArenaMetric) {
        final Map<String, Object> metrics = new HashMap<>();

        /**
         * Returns the number of thread caches backed by this arena.
         */
        metrics.put("1_numThreadCaches", poolArenaMetric.numThreadCaches());

        /**
         * Return the number of allocations done via the arena. This includes all sizes.
         */
        metrics.put("2_0_numAllocations", poolArenaMetric.numAllocations());

        /**
         * Return the number of tiny allocations done via the arena.
         */
        metrics.put("2_1_numTinyAllocations", poolArenaMetric.numTinyAllocations());

        /**
         * Return the number of small allocations done via the arena.
         */
        metrics.put("2_2_numSmallAllocations", poolArenaMetric.numSmallAllocations());

        /**
         * Return the number of normal allocations done via the arena.
         */
        metrics.put("2_3_numNormalAllocations", poolArenaMetric.numNormalAllocations());

        /**
         * Return the number of huge allocations done via the arena.
         */
        metrics.put("2_4_numHugeAllocations", poolArenaMetric.numHugeAllocations());

        /**
         * Return the number of deallocations done via the arena. This includes all sizes.
         */
        metrics.put("3_0_numDeallocations", poolArenaMetric.numDeallocations());

        /**
         * Return the number of tiny deallocations done via the arena.
         */
        metrics.put("3_1_numTinyDeallocations", poolArenaMetric.numTinyDeallocations());

        /**
         * Return the number of small deallocations done via the arena.
         */
        metrics.put("3_2_numSmallDeallocations", poolArenaMetric.numSmallDeallocations());

        /**
         * Return the number of normal deallocations done via the arena.
         */
        metrics.put("3_3_numNormalDeallocations", poolArenaMetric.numNormalDeallocations());

        /**
         * Return the number of huge deallocations done via the arena.
         */
        metrics.put("3_4_numHugeDeallocations", poolArenaMetric.numHugeDeallocations());

        /**
         * Return the number of currently active allocations.
         */
        metrics.put("4_0_numActiveAllocations", poolArenaMetric.numActiveAllocations());

        /**
         * Return the number of currently active tiny allocations.
         */
        metrics.put("4_1_numActiveTinyAllocations", poolArenaMetric.numActiveTinyAllocations());

        /**
         * Return the number of currently active small allocations.
         */
        metrics.put("4_2_numActiveSmallAllocations", poolArenaMetric.numActiveSmallAllocations());

        /**
         * Return the number of currently active normal allocations.
         */
        metrics.put("4_3_numActiveNormalAllocations", poolArenaMetric.numActiveNormalAllocations());

        /**
         * Return the number of currently active huge allocations.
         */
        metrics.put("4_4_numActiveHugeAllocations", poolArenaMetric.numActiveHugeAllocations());

        /**
         * Return the number of active bytes that are currently allocated by the arena.
         */
        metrics.put("5_numActiveBytes", poolArenaMetric.numActiveBytes());

        /**
         * Returns the number of chunk lists for the arena.
         */
        metrics.put("6_0_numChunkLists", poolArenaMetric.numChunkLists());

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolChunkListMetric}s.
         */
        metrics.put("6_1_chunkLists", metricsOfChunkLists(poolArenaMetric.chunkLists()));

        /**
         * Returns the number of tiny sub-pages for the arena.
         */
        metrics.put("7_0_numTinySubpages", poolArenaMetric.numTinySubpages());

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for tiny sub-pages.
         */
        metrics.put("7_1_tinySubpages", metricsOfSubpages(
                poolArenaMetric.numTinySubpages(),
                poolArenaMetric.tinySubpages()));

        /**
         * Returns the number of small sub-pages for the arena.
         */
        metrics.put("8_0_numSmallSubpages", poolArenaMetric.numSmallSubpages());

        /**
         * Returns an unmodifiable {@link List} which holds {@link PoolSubpageMetric}s for small sub-pages.
         */
        metrics.put("8_1_smallSubpage", metricsOfSubpages(
                poolArenaMetric.numSmallSubpages(),
                poolArenaMetric.smallSubpages()));

        return metrics;
    }

    private static Map<String, Object> metricsOfChunkLists(final List<PoolChunkListMetric> chunkLists) {
        final Map<String, Object> metrics = new HashMap<>();
        int idx = 0;
        for (PoolChunkListMetric chunkListMetric : chunkLists) {
            metrics.put(idx++ + "_chunkList", metricsOfPoolChunkList(chunkListMetric));
        }
        return metrics;
    }

    private static Map<String, Object> metricsOfPoolChunkList(final PoolChunkListMetric chunkListMetric) {
        final Map<String, Object> metrics = new HashMap<>();

        metrics.put("1_minUsage", chunkListMetric.minUsage());
        metrics.put("2_maxUsage", chunkListMetric.maxUsage());

        final Iterator<PoolChunkMetric> iter = chunkListMetric.iterator();
        int idx = 0;
        while (iter.hasNext()) {
            metrics.put("chunk_" + idx++, metricsOfPoolChunk(iter.next()));
        }
        metrics.put("3_numChunks", idx);
        return metrics;
    }

    private static Map<String, Object> metricsOfPoolChunk(final PoolChunkMetric chunkMetric) {
        final Map<String, Object> metrics = new HashMap<>();
        metrics.put("1_usage", chunkMetric.usage());
        metrics.put("2_freeBytes", chunkMetric.freeBytes());
        metrics.put("3_chunkSize", chunkMetric.chunkSize());
        return metrics;
    }

    private static Map<String, Object> metricsOfSubpages(final int count, final List<PoolSubpageMetric> subpages) {
        final Map<String, Object> metrics = new HashMap<>();

        final int w = (int) (Math.log10(count)) + 1;
        final String fstr = "%0" + w + "d";

        int idx = 0;
        for (PoolSubpageMetric subpageMetric : subpages) {
            metrics.put(String.format(fstr, idx++) + "_tinySubpage", metricsOfPoolSubpage(subpageMetric));
        }
        return metrics;
    }

    private static Map<String, Object> metricsOfPoolSubpage(final PoolSubpageMetric subpageMetric) {
        final Map<String, Object> metrics = new HashMap<>();
        /**
         * Return the number of maximal elements that can be allocated out of the sub-page.
         */
        metrics.put("2_maxNumElements", subpageMetric.maxNumElements());

        /**
         * Return the number of available elements to be allocated.
         */
        metrics.put("3_numAvailable", subpageMetric.numAvailable());

        /**
         * Return the size (in bytes) of the elements that will be allocated.
         */
        metrics.put("1_elementSize", subpageMetric.elementSize());

        /**
         * Return the size (in bytes) of this page.
         */
        metrics.put("4_pageSize", subpageMetric.pageSize());

        return metrics;
    }

    public String getMetricAsString() {
        return PooledByteBufAllocator.DEFAULT.dumpStats();
    }
}
