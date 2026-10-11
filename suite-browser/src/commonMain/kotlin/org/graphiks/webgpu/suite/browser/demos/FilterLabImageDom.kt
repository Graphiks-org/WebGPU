@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlin.ExperimentalUnsignedTypes::class)
package org.graphiks.webgpu.suite.browser.demos

import js.coroutines.resumeWithError
import js.promise.Promise
import kotlin.coroutines.resume
import kotlin.js.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.graphiks.webgpu.suite.demos.filterlab.FilterImage

internal external interface DecodedFilterImage : JsAny {
    val width: Int
    val height: Int
    val originalWidth: Int
    val originalHeight: Int
    val name: String
    val data: JsAny
}

internal fun selectedFilterFile(input: DomElement): JsAny? = js("input.files && input.files[0] || null")
private fun imageByte(data: JsAny, index: Int): Int = js("data[index]")
private fun decodeImagePromise(file: JsAny, maxDimension: Int): Promise<DecodedFilterImage> = js("""Promise.resolve().then(function () {
    if (!file.size || file.size > 20 * 1024 * 1024) throw new Error('file-size');
    return file.slice(0, 12).arrayBuffer();
}).then(function (header) {
    const bytes = new Uint8Array(header);
    const png = bytes.length >= 8 && [137,80,78,71,13,10,26,10].every((v,i) => bytes[i] === v);
    const jpeg = bytes.length >= 3 && bytes[0] === 255 && bytes[1] === 216 && bytes[2] === 255;
    const webp = bytes.length >= 12 && String.fromCharCode(...bytes.slice(0,4)) === 'RIFF' && String.fromCharCode(...bytes.slice(8,12)) === 'WEBP';
    if (!((file.type === 'image/png' && png) || (file.type === 'image/jpeg' && jpeg) || (file.type === 'image/webp' && webp))) throw new Error('file-format');
    return createImageBitmap(file, { imageOrientation: 'from-image', premultiplyAlpha: 'none', colorSpaceConversion: 'default' });
}).then(function (bitmap) {
    try {
        if (!bitmap.width || !bitmap.height) throw new Error('file-dimensions');
        const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height));
        const width = Math.max(1, Math.floor(bitmap.width * scale));
        const height = Math.max(1, Math.floor(bitmap.height * scale));
        const canvas = document.createElement('canvas'); canvas.width = width; canvas.height = height;
        const context = canvas.getContext('2d', { willReadFrequently: true });
        if (!context) throw new Error('image-context');
        context.drawImage(bitmap, 0, 0, width, height);
        return { width, height, originalWidth: bitmap.width, originalHeight: bitmap.height,
            name: file.name, data: context.getImageData(0, 0, width, height).data };
    } finally { bitmap.close(); }
})""")

internal suspend fun decodeFilterImage(file: JsAny, maxDimension: Int): DecodedFilterImage =
    suspendCancellableCoroutine { continuation ->
        decodeImagePromise(file,maxDimension).then(onFulfilled = { value -> continuation.resume(value); null },
            onRejected = { error -> continuation.resumeWithError(error); null })
    }

internal fun DecodedFilterImage.toFilterImage(): FilterImage =
    FilterImage(width,height,UByteArray(width*height*4) { imageByte(data,it).toUByte() })

internal suspend fun decodeSelectedImage(file: JsAny, maxDimension: Int): FilterImage =
    decodeFilterImage(file,maxDimension).toFilterImage()
