package io.ygdrasil.webgpu.fetcher

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class AtomicFileReplacer(private val move: FileMoveOperation = NioFileMoveOperation()) {
    fun replaceResource(stagedFile: Path, targetFile: Path, operationContext: String) {
        try {
            move.move(stagedFile, targetFile, atomic = true)
        } catch (_: AtomicMoveNotSupportedException) {
            move.move(stagedFile, targetFile, atomic = false)
        }
    }
}
