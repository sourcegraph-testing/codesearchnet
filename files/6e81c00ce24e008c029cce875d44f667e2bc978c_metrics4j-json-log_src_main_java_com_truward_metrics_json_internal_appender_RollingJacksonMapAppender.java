package com.truward.metrics.json.internal.appender;

import com.truward.metrics.json.internal.cache.RecordCache;
import com.truward.metrics.time.TimeService;
import com.truward.metrics.json.settings.CompressionType;
import com.truward.metrics.json.settings.TimeBasedRollingLogSettings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Appender, that takes into an account time-based rolling settings,
 * see {@link com.truward.metrics.json.settings.TimeBasedRollingLogSettings}.
 * <p>THIS CLASS IS NOT A PART OF THE PUBLIC API.</p>
 *
 * @author Alexander Shabanov
 */
public final class RollingJacksonMapAppender extends AbstractJacksonMapAppender {
  public static final int DEFAULT_BUFFER_SIZE = 4096;
  private static final String DEFAULT_DATE_TIME_SUFFIX = "yyyy-MM-dd_HH_mm_ss";
  private static final long MAX_COMPRESSION_THREAD_WAIT_TIME = 500L;

  // mutable class state
  private volatile File currentFile;
  private volatile OutputStream currentStream;
  private volatile Thread currentCompressingThread;
  private long now;
  private volatile long lastTimeMillis;

  // immutable variables
  private final TimeBasedRollingLogSettings settings;
  private final long maxTimeDeltaMillis;
  private final Compressor compressor;
  private final String compressedFileSuffix;
  private final String tempCompressedFileSuffix;
  private final TimeService timeService;
  private final DateFormat dateFormat; // access is synchronized

  public RollingJacksonMapAppender(@Nonnull TimeBasedRollingLogSettings settings,
                                   @Nonnull RecordCache recordCache) {
    super(recordCache);
    this.settings = settings;
    this.dateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_SUFFIX);
    this.maxTimeDeltaMillis = settings.getTimeDeltaMillis();
    this.timeService = settings.getTimeService();
    this.compressor = getCompressor(settings.getCompressionType());
    if (compressor != null) {
      compressedFileSuffix = '.' + compressor.getExtension();
      tempCompressedFileSuffix = compressedFileSuffix + ".temp";
    } else {
      compressedFileSuffix = null;
      tempCompressedFileSuffix = null;
    }
  }

  @Override protected void onClose() throws IOException {
    OutputStream stream = currentStream;
    // remove reference to current stream
    currentStream = null;

    if (stream != null) {
      stream.close();
    }

    // remove reference to current file
    currentFile = null;

    // wait compressing thread to stop and remove reference to it
    final Thread thread = currentCompressingThread;
    if (thread != null) {
      try {
        thread.join(MAX_COMPRESSION_THREAD_WAIT_TIME);
      } catch (InterruptedException e) {
        log.error("Waiting for compression thread to close has been interrupted", e);
      }
    }
    currentCompressingThread = null;
  }

  @Nonnull @Override protected OutputStream getOutputStream() {
    final OutputStream result = currentStream;
    if (result == null) {
      throw new IllegalStateException("currentStream is null"); // shouldn't happen
    }
    return result;
  }

  @Override protected void onWritePrepare() {
    now = timeService.now();
  }

  @Override protected void onWriteStart() {
    if (lastTimeMillis == 0) {
      lastTimeMillis = now; // just update current time
    } else if ((now - lastTimeMillis) >= maxTimeDeltaMillis) {
      // roll log and update time
      lastTimeMillis = now;
      rollLog();
    }

    if (currentFile == null) {
      startNewFile();
    }

    assert currentFile != null && currentStream != null;
  }

  private void startNewFile() {
    currentFile = findNewFile(settings.getFileNameBase() + '_' + dateFormat.format(new Date(now)),
        settings.getSuffix());

    // Open stream
    try {
      currentStream = new FileOutputStream(currentFile, false);
    } catch (IOException e) {
      log.error("Unable to write into a file {}", currentFile.getAbsolutePath(), e);
      currentStream = NullOutputStream.INSTANCE; // write won't make any effect
    }
  }

  private static class NullOutputStream extends OutputStream {
    static final NullOutputStream INSTANCE = new NullOutputStream();

    NullOutputStream() {}

    @Override public void write(int b) throws IOException {
      // do nothing
    }
  }

  /**
   * Writes older log into the file and updates current output stream
   */
  private void rollLog() {
    final File file = currentFile;
    if (file == null) {
      throw new IllegalStateException("currentFile is null"); // shouldn't happen
    }

    final OutputStream stream = currentStream;
    if (stream == null) {
      throw new IllegalStateException("currentStream is null"); // shouldn't happen
    }

    // reset class members
    this.currentFile = null;
    this.currentStream = null;

    // compress file, if needed
    if (compressor == null) {
      return; // compression is not needed
    }

    // start new compression thread
    final Thread compressingThread = new Thread(new Runnable() {
      @Override
      public void run() {
        log.trace("Closing output stream of target file={}", file);

        // close old stream
        try {
          stream.close();
        } catch (IOException e) {
          log.error("Unable to properly close output stream", e);
        }

        // compress file contents
        compressFileContents(compressor, file);

        // mark current thread as null
        currentCompressingThread = null;
      }
    });
    compressingThread.run();
    currentCompressingThread = compressingThread;
  }

  private void compressFileContents(@Nonnull Compressor compressor, @Nonnull File file) {
    log.trace("Starting compressing contents of file={}", file);

    final File targetFile = findNewFile(file.getAbsolutePath(), compressedFileSuffix);
    final File tempFile = findNewFile(file.getAbsolutePath(), tempCompressedFileSuffix);

    // compress source file contents and write result to the temp file
    try (final InputStream sourceStream = new FileInputStream(file)) {
      try (final OutputStream tempFileStream = new FileOutputStream(tempFile)) {
        try (final OutputStream compressionStream = compressor.openOutputStream(tempFileStream, file.getName())) {
          final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

          for (;;) {
            int read = sourceStream.read(buffer);
            if (read < 0) {
              break;
            }

            compressionStream.write(buffer, 0, read);
          }
        }
      }
    } catch (IOException e) {
      log.error("Unable to compress {} to {}", file.getAbsolutePath(), tempFile.getAbsoluteFile(), e);

      // delete temp file
      if (tempFile.exists() && !tempFile.delete()) {
        log.error("Unable to remove temp file {}", tempFile.getAbsolutePath());
      }

      return;
    }

    if (!tempFile.renameTo(targetFile)) {
      log.error("Can't rename {} to {}", tempFile.getAbsolutePath(), targetFile.getAbsoluteFile());
      return;
    }

    // now remove old file
    if (!file.delete()) {
      log.error("Unable to remove uncompressed log file={}", file.getAbsolutePath());
    }

    // rename succeeded
    log.trace("Compressed log file has been successfully created: {}", targetFile);
  }

  @Nonnull private static File findNewFile(@Nonnull String leadingFileNamePart, @Nonnull String suffix) {
    final StringBuilder builder = new StringBuilder(leadingFileNamePart.length() + 30);
    builder.append(leadingFileNamePart);

    final int prevLen = builder.length();
    File newFile;
    for (int index = 0;; ++index) {
      builder.setLength(prevLen);
      if (index > 0) {
        builder.append('_').append(index);
      }
      builder.append(suffix);

      newFile = new File(builder.toString());
      if (!newFile.exists()) {
        return newFile;
      } // else - continue
    }
  }

  @Nullable private Compressor getCompressor(@Nonnull CompressionType compressionType) {
    switch (compressionType) {
      case GZIP:
        return GzipCompressor.INSTANCE;

      case ZIP:
        return ZipCompressor.INSTANCE;

      case NONE:
        break;

      default:
        log.error("Unknown compressionType={}", compressionType); // shouldn't trigger
    }

    return null;
  }

  private interface Compressor {
    @Nonnull String getExtension();
    @Nonnull OutputStream openOutputStream(@Nonnull OutputStream outputStream,
                                           @Nonnull String fileName) throws IOException;
  }

  private static final class GzipCompressor implements Compressor {
    static final GzipCompressor INSTANCE = new GzipCompressor();

    @Nonnull @Override public String getExtension() {
      return "gz";
    }

    @Nonnull @Override public OutputStream openOutputStream(@Nonnull OutputStream outputStream,
                                                            @Nonnull String fileName) throws IOException {
      return new GZIPOutputStream(outputStream, 4096);
    }
  }

  private static final class ZipCompressor implements Compressor {
    static final ZipCompressor INSTANCE = new ZipCompressor();

    @Nonnull @Override public String getExtension() {
      return "zip";
    }

    @Nonnull @Override public OutputStream openOutputStream(@Nonnull OutputStream outputStream,
                                                            @Nonnull String fileName) throws IOException {
      final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
      zipOutputStream.putNextEntry(new ZipEntry(fileName));
      return zipOutputStream;
    }
  }
}
