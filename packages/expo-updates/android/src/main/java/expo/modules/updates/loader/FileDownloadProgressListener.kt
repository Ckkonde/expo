package expo.modules.updates.loader

import okhttp3.ResponseBody
import expo.modules.updates.db.entity.AssetEntity
import okhttp3.MediaType
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

interface FileDownloadProgressListener {
  fun update(bytesRead: Long, contentLength: Long)
}

internal class FileDownloadProgressResponseBody(
  private val responseBody: ResponseBody,
  private val progressListener: FileDownloadProgressListener
) : ResponseBody() {

  private var bufferedSource: BufferedSource? = null

  override fun contentType(): MediaType? = responseBody.contentType()

  override fun contentLength(): Long = responseBody.contentLength()

  override fun source(): BufferedSource {
    if (bufferedSource == null) {
      bufferedSource = source(responseBody.source()).buffer()
    }
    return bufferedSource!!
  }

  private fun source(source: Source): Source {
    return object : ForwardingSource(source) {
      var totalBytesRead: Long = 0

      override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = super.read(sink, byteCount)
        totalBytesRead += if (bytesRead != -1L) bytesRead else 0
        progressListener.update(totalBytesRead, responseBody.contentLength())
        return bytesRead
      }
    }
  }
}
