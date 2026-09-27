package org.graphiks.webgpu.fetcher

import java.nio.file.Path

class AtomicCacheWriter(private val move: FileMoveOperation = NioFileMoveOperation()) {
    fun replace(stagedCache: Path, cacheFile: Path, operationContext: String) {
        move.move(stagedCache, cacheFile, atomic = true)
    }
}
