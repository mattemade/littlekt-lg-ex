package com.littlekt.file.vfs

import com.littlekt.audio.AudioClip
import com.littlekt.audio.AudioClipEx
import com.littlekt.audio.AudioStream
import com.littlekt.audio.AudioStreamEx
import com.littlekt.audio.WebAudioClip
import com.littlekt.audio.WebAudioClipEx
import com.littlekt.audio.WebAudioEx
import com.littlekt.audio.WebAudioStream
import com.littlekt.audio.WebAudioStreamEx
import com.littlekt.file.Base64.encodeToBase64
import com.littlekt.file.ByteBufferImpl
import com.littlekt.graphics.Pixmap
import com.littlekt.graphics.Texture
import com.littlekt.graphics.gl.PixmapTextureData
import com.littlekt.graphics.gl.TexMagFilter
import com.littlekt.graphics.gl.TexMinFilter
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image

/**
 * @author Colton Daily
 * @date 12/20/2021
 */

/**
 * Loads an image from the path as a [Texture]. This will call [Texture.prepare] before returning!
 * @return the loaded texture
 */
actual suspend fun VfsFile.readTexture(minFilter: TexMinFilter, magFilter: TexMagFilter, mipmaps: Boolean): Texture {
    val data = PixmapTextureData(readPixmap(), mipmaps)
    return Texture(data).also {
        it.minFilter = minFilter
        it.magFilter = magFilter
        it.prepare(vfs.context)
    }
}

/**
 * Reads Base64 encoded ByteArray for embedded images.
 */
internal actual suspend fun ByteArray.readPixmap(): Pixmap {
    val path = "data:image/png;base64,${encodeToBase64()}"

    return readPixmap(path)
}


/**
 * Loads an image from the path as a [Pixmap].
 * @return the loaded texture
 */
actual suspend fun VfsFile.readPixmap(): Pixmap {
    return readPixmap(path)
}

private suspend fun readPixmap(path: String): Pixmap {
    val deferred = CompletableDeferred<Image>()

    val img = Image()
    img.onload = {
        deferred.complete(img)
    }
    img.onerror = { _, _, _, _, _ ->
        if (path.startsWith("data:")) {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from data URL"))
        } else {
            deferred.completeExceptionally(RuntimeException("Failed loading tex from ${path}"))
        }
    }
    img.crossOrigin = ""
    img.src = path

    val loadedImg = deferred.await()
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = loadedImg.width
    canvas.height = loadedImg.height
    val canvasCtx = canvas.getContext("2d") as CanvasRenderingContext2D

    val w = loadedImg.width.toDouble()
    val h = loadedImg.height.toDouble()
    canvasCtx.drawImage(img, 0.0, 0.0, w, h, 0.0, 0.0, w, h)
    val pixels = ByteBufferImpl(canvasCtx.getImageData(0.0, 0.0, w, h).data)

    return Pixmap(loadedImg.width, loadedImg.height, pixels)
}

/**
 * Loads audio from the path as an [AudioClip].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClip(): AudioClip {
    val url = if (isHttpUrl()) path else "${vfs.baseDir}/$path"
    val webAudio = WebAudioEx.create(vfs.job, url, vfs.logger)
    return if (webAudio != null) {
        WebAudioClipEx(webAudio)
    } else {
        WebAudioClip(url)
    }
}

/**
 * Loads audio from the path as an [AudioClipEx].
 *
 * @return the loaded audio clip
 */
actual suspend fun VfsFile.readAudioClipEx(): AudioClipEx {
    val url = if (isHttpUrl()) path else "${vfs.baseDir}/$path"
    return WebAudioClipEx(WebAudioEx.create(vfs.job, url, vfs.logger)?: error("Could not create WebAudioEx"))
}

/**
 * Streams audio from the path as an [AudioStream].
 *
 * @return a new [AudioStream]
 */
actual suspend fun VfsFile.readAudioStream(): AudioStream {
    val url = if (isHttpUrl()) path else "${vfs.baseDir}/$path"
    val webAudio = WebAudioEx.create(vfs.job, url, vfs.logger)
    return if (webAudio != null) {
        WebAudioStreamEx(webAudio)
    } else {
        WebAudioStream(url)
    }
}

actual suspend fun VfsFile.readAudioStreamEx(): AudioStreamEx {
    val url = if (isHttpUrl()) path else "${vfs.baseDir}/$path"
    return WebAudioStreamEx(WebAudioEx.create(vfs.job, url, vfs.logger)?: error("Could not create WebAudioEx"))
}

actual suspend fun VfsFile.writePixmap(pixmap: Pixmap) {
    TODO("IMPLEMENT ME")
}
