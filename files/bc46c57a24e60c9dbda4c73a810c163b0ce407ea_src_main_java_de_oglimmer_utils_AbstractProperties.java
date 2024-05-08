package de.oglimmer.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A base class for application property classes. Supports json-based config
 * files, automatic reloads when the file changes, merging in-classpath and
 * out-of-classpath files and can take the whole configuration from a string to
 * support unit test configurations.
 * 
 * All property files must be in JSON format.
 * 
 * <div> Search-Logic:
 * <ol>
 * <li>Always load default properties (filename: defaultPropertyFile) from
 * classpath</li>
 * <li>if systemPropertiesKey starts with "memory:", treat systemPropertiesKey
 * as a JSON object and merge it</li>
 * <li>else if System.getProperties(systemPropertiesKey) is not null =&gt; load
 * the file coming from this and merge it</li>
 * <li>else if file /etc/${systemPropertiesKey} exists =&gt; load and merge
 * it</li>
 * </ol>
 * </div>
 * 
 * @author Oli Zimpasser
 *
 */
@Slf4j
public class AbstractProperties {

	private static final boolean DEBUG = false;

	final private String systemPropertiesKey;
	final private String defaultPropertyFile;

	@Getter(value = AccessLevel.PROTECTED)
	private JsonObject json = Json.createObjectBuilder().build();
	private volatile boolean running = true;
	private Thread propertyFileWatcherThread;
	final private List<Runnable> reloadables = Collections.synchronizedList(new ArrayList<>());
	private String sourceLocation;
	private boolean reload;

	/**
	 * Uses /default.properties from classpath for default properties file. Reload will be enabled.
	 * 
	 * All property files must be in JSON format.
	 * 
	 * @param systemPropertiesKey name of a -D parameter which holds a filesystem (not classpath) filename.
	 */
	protected AbstractProperties(String systemPropertiesKey) {
		this(systemPropertiesKey, "/default.properties");
	}

	/**
	 * Reload will be enabled.
	 * 
	 * All property files must be in JSON format.
	 * 
	 * @param systemPropertiesKey name of a -D parameter which holds a filesystem (not classpath) filename.
	 * @param defaultPropertyFile classpath based filename to the default properties file
	 */
	protected AbstractProperties(String systemPropertiesKey, String defaultPropertyFile) {
		this(systemPropertiesKey, defaultPropertyFile, true);
	}

	/**
	 * All property files must be in JSON format.
	 * 
	 * @param systemPropertiesKey name of a -D parameter which holds a filesystem (not classpath) filename.
	 * @param defaultPropertyFile classpath based filename to the default properties file
	 * @param reload enable/disable a file watcher to automatically reload the config
	 */
	protected AbstractProperties(String systemPropertiesKey, String defaultPropertyFile, boolean reload) {
		this.systemPropertiesKey = systemPropertiesKey;
		this.defaultPropertyFile = defaultPropertyFile;
		this.reload = reload;
		init();
	}

	/**
	 * Overwrite this to manipulate the properties after default and extra properties are loaded.
	 * 
	 * @return the manipulated properties
	 */
	protected JsonObject createExtraInitAttributes() {
		return json;
	}

	private void initMemory() {
		final String memoryConfigStr = sourceLocation.substring("memory:".length());
		final InputStream is = new ByteArrayInputStream(memoryConfigStr.getBytes(StandardCharsets.UTF_8));
		mergeJson(is);
	}

	private boolean initExternalFile() {
		try (final InputStream fis = new FileInputStream(sourceLocation)) {
			mergeJson(fis);
			return true;
		} catch (IOException e) {
			log.error("Failed to load properties file " + sourceLocation, e);
		}
		return false;
	}

	private boolean initExternalEtcFile() {
		File extDefaultFile = new File("/etc/" + systemPropertiesKey);
		if (extDefaultFile.exists()) {
			sourceLocation = extDefaultFile.getAbsolutePath();
			return initExternalFile();
		}
		return false;
	}

	private void init() {
		sourceLocation = System.getProperty(systemPropertiesKey);
		loadDefaultProperties();
		loadExtraPropertiesAndStartWatcherThread();
		json = createExtraInitAttributes();
		if (DEBUG) {
			System.out.println("Used config: " + prettyPrint(json));
		}
	}

	private void loadExtraPropertiesAndStartWatcherThread() {
		boolean startFileWatcher = false;
		if (sourceLocation != null) {
			if (sourceLocation.startsWith("memory:")) {
				initMemory();
			} else {
				startFileWatcher = initExternalFile();
			}
		} else {
			startFileWatcher = initExternalEtcFile();
		}
		startWatcherThread(startFileWatcher);
	}

	private void startWatcherThread(boolean startFileWatcher) {
		if (startFileWatcher && reload && propertyFileWatcherThread == null) {
			propertyFileWatcherThread = new Thread(new PropertyFileWatcher());
			propertyFileWatcherThread.start();
		}
	}

	private void loadDefaultProperties() {
		try (final JsonReader rdr = Json.createReader(this.getClass().getResourceAsStream(defaultPropertyFile))) {
			json = rdr.readObject();
			System.out.println("Successfully loaded properties from " + defaultPropertyFile);
		}
	}

	private String prettyPrint(final JsonObject json) {
		final Map<String, Object> properties = new HashMap<>(1);
		properties.put(JsonGenerator.PRETTY_PRINTING, true);
		final JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

		final StringWriter sw = new StringWriter();
		try (final JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
			jsonWriter.writeObject(json);

		}
		return sw.toString();
	}

	private void mergeJson(final InputStream is) {
		try (final JsonReader rdr = Json.createReader(is)) {
			final JsonObject toBeMerged = rdr.readObject();
			json = merge(json, toBeMerged);
			System.out.println("Successfully loaded " + systemPropertiesKey + " from " + sourceLocation + " for merge");
		}
	}

	protected JsonObject merge(final JsonObject base, final JsonObject toOverwrite) {
		final JsonObjectBuilder job = Json.createObjectBuilder();
		for (final Entry<String, JsonValue> entry : base.entrySet()) {
			final String key = entry.getKey();
			if (toOverwrite.containsKey(key)) {
				job.add(key, toOverwrite.get(key));
			} else {
				job.add(key, entry.getValue());
			}
		}
		for (final Entry<String, JsonValue> entry : toOverwrite.entrySet()) {
			final String key = entry.getKey();
			// if JsonObjectBuilder would have a containsKey we could skip all
			// already existing keys
			job.add(key, toOverwrite.get(key));
		}
		return job.build();
	}

	/**
	 * Register your callbacks here to get informed when the property file has changed
	 * 
	 * @param toCall
	 *            a runnable to be called
	 */
	public void registerOnReload(final Runnable toCall) {
		if (!reload) {
			throw new RuntimeException("Not allowed as reload is false");
		}
		reloadables.add(toCall);
	}

	void reload() {
		init();
		reloadables.forEach(Runnable::run);
	}

	/**
	 * Call do stop the watch thread
	 */
	public void shutdown() {
		running = false;
		if (propertyFileWatcherThread != null) {
			propertyFileWatcherThread.interrupt();
		}
	}

	class PropertyFileWatcher implements Runnable {

		public void run() {
			final File toWatch = new File(sourceLocation);
			try {
				final Path path = FileSystems.getDefault().getPath(toWatch.getParent());
				log.info("PropertyFileWatcher started on {}", path);
				loopAndWatch(toWatch, path);
			} catch (InterruptedException e) {
			} catch (Exception e) {
				log.error("PropertyFileWatcher failed", e);
			}
			log.info("PropertyFileWatcher ended");
		}

		private void loopAndWatch(final File toWatch, final Path path) throws IOException, InterruptedException {
			try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
				path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
				while (running) {
					final WatchKey wk = watchService.take();
					processWatchKey(toWatch, wk);
				}
			}
		}

		private void processWatchKey(final File toWatch, final WatchKey wk) throws InterruptedException {
			for (final WatchEvent<?> event : wk.pollEvents()) {
				// we only register "ENTRY_MODIFY" so the context is always a Path.
				final Path changed = (Path) event.context();
				if (changed.endsWith(toWatch.getName())) {
					log.debug("{} changed => reload", toWatch.getAbsolutePath());
					reload();
				}
			}
			boolean valid = wk.reset();
			if (!valid) {
				log.warn("The PropertyFileWatcher's key has been unregistered.");
			}
		}
	}

}
