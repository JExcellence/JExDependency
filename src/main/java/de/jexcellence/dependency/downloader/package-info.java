/**
 * Downloads artifacts into the server-wide cache.
 *
 * <p>{@link de.jexcellence.dependency.downloader.DependencyDownloader} tries repositories in this order:
 * repositories added with {@code addRepository}, the Maven Central mirror, Maven Central, repositories declared in
 * {@code dependencies.yml}, and the built-in community repositories when they are turned on. A dependency pinned
 * with {@code coordinate @ url} comes only from that repository. Requests share one HTTP/2 client with a per-host
 * limit; transient errors are retried with backoff.
 *
 * <p>Every download is streamed to a temporary file, must open as a jar, must match a pinned SHA-256 or the
 * repository's {@code .sha256} or {@code .sha1} file, and is then moved atomically into
 * {@code <server>/.jexdependency/cache/jars}. Empty, truncated and HTML responses are rejected, so other threads
 * and servers only ever see complete jars. {@link de.jexcellence.dependency.downloader.DownloadThreads} provides
 * virtual threads where the runtime has them.
 */
package de.jexcellence.dependency.downloader;
