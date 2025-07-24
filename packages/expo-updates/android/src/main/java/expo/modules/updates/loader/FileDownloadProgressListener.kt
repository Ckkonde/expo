package expo.modules.updates.loader

import okhttp3.ResponseBody
import okhttp3.MediaType
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

internal interface FileDownloadProgressListener {
  fun update(bytesRead: Long, contentLength: Long) {
    // Only emit progress if content length is known
    if (contentLength > 0) {
      onProgressUpdate(bytesRead.toDouble() / contentLength.toDouble())
    }
  }
  
  fun onProgressUpdate(progress: Double) {}
}

internal class FileDownloadProgressResponseBody(
  private val responseBody: ResponseBody,
  private val progressListener: FileDownloadProgressListener
) : ResponseBody() {

  override fun contentType(): MediaType? = responseBody.contentType()

  override fun contentLength(): Long = responseBody.contentLength()

  private val bufferedSource by lazy {
    source(responseBody.source()).buffer()
  }

  override fun source(): BufferedSource = bufferedSource

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
