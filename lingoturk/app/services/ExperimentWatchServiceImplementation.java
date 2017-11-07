package services;

import controllers.RenderController;
import org.apache.commons.io.FileUtils;
import play.inject.ApplicationLifecycle;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Implements the ExperimentWatchService. If new experiment types are created we start
 * monitoring the files for changes or if existing types are deleted, the monitoring
 * is stopped.
 */
public class ExperimentWatchServiceImplementation implements ExperimentWatchService {

    private static class FileWatcher implements Runnable {

        private LingoturkConfig lingoturkConfig;
        private PathMatcher renderMatcher = FileSystems.getDefault().getPathMatcher("glob:*_render.html");
        private PathMatcher previewMatcher = FileSystems.getDefault().getPathMatcher("glob:*_preview.html");
        private ConcurrentHashMap<WatchKey, Path> directories = new ConcurrentHashMap<>();
        private ConcurrentHashMap<String, WatchKey> experiments = new ConcurrentHashMap<>();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private WatchService watcher = null;

        public FileWatcher(LingoturkConfig lingoturkConfig) {
            this.lingoturkConfig = lingoturkConfig;
            try {
                watcher = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not create WatchService.");
            }

            for (String name : lingoturkConfig.getExperimentNames()) {
                if (!registerExperiment(name)) {
                    System.err.println("Could not load experiment view for: " + name);
                }
            }
        }

        /**
         * Reloads the experiment content for experiment type {@code name}. Updated
         * contents will be put into the RenderController map.
         *
         * @param name The name of the experiment
         * @throws IOException Propagated from FileUtils if content cannot be read.
         */
        private void reloadExperimentType(String name) throws IOException {
            String content = FileUtils.readFileToString(Paths.get(lingoturkConfig.getPathPrefix() + "app/views/ExperimentRendering/", name, name + "_render.html").toFile(), "UTF-8");
            RenderController.experimentContent.put(name, content);

            File previewFile = Paths.get(lingoturkConfig.getPathPrefix() + "app/views/ExperimentRendering/", name, name + "_preview.html").toFile();
            if (previewFile.exists()) {
                content = FileUtils.readFileToString(previewFile, "UTF-8");
                RenderController.previewContent.put(name, content);
            }
        }

        /**
         * Reloads a single experiment file {@code f} for experiment type {@code name}.
         *
         * @param f                   The file containing the experiment's content
         * @param name                The name of the experiment type
         * @param isExperimentContent Whether the file contains the content of the experiment itself or the preview
         * @throws IOException Propagated from FileUtils if content cannot be read.
         */
        private void reloadFile(File f, String name, boolean isExperimentContent) throws IOException {
            String content = FileUtils.readFileToString(f, "UTF-8");
            if (isExperimentContent) {
                RenderController.experimentContent.put(name, content);
            } else {
                RenderController.previewContent.put(name, content);
            }
        }

        /**
         * Stops the watch service
         */
        public void stop() {
            stopped.set(true);
            if (watcher != null) {
                try {
                    watcher.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Could not close Watchservice. Experiment updates will not be registered.");
                }
            }
        }

        /**
         * Unregisters the experiment type {@code experimentName}. Changes won't be reloaded anymore
         *
         * @param experimentName The name of the experiment
         */
        public void unregisterExperiment(String experimentName) {
            WatchKey key = experiments.get(experimentName);
            key.cancel();
            directories.remove(key);
            experiments.remove(experimentName);
        }

        /**
         * Registers a new experiment type {@code experimentName}.
         *
         * @param experimentName The name of the new experiment type
         * @return Whether the experiment type could be registered
         */
        public boolean registerExperiment(String experimentName) {
            Path dir = Paths.get(lingoturkConfig.getPathPrefix() + "app/views/ExperimentRendering", experimentName);
            try {
                reloadExperimentType(experimentName);
                WatchKey key = dir.register(watcher, ENTRY_MODIFY);
                directories.put(key, dir);
                experiments.put(experimentName, key);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * The main function of the thread. Constantly waits for new events to happen and reloads
         * the data when needed.
         */
        @Override
        public void run() {
            while (!stopped.get()) {

                // wait for key to be signaled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (ClosedWatchServiceException | InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }

                    // The filename is the context of the event.
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path directory = directories.get(key);

                    boolean isPreviewFile = previewMatcher.matches(filename);
                    boolean isRenderFile = renderMatcher.matches(filename);

                    // Match only the experiment files (and not any tmp files or other stuff)
                    if (isPreviewFile || isRenderFile) {
                        File modifiedFile = directory.resolve(filename).toFile();
                        try {
                            // Hack to make sure that file has been written successfully before trying to open it.
                            // Otherwise a crash might occur.
                            Thread.sleep(50);
                            reloadFile(modifiedFile, filename.toString().replace("_preview.html", "").replace("_render.html", ""), isRenderFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.err.println("Could not reload experiment content. Changes will be unreflected.");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            try {
                watcher.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not close watch service.");
            }
        }
    }

    private FileWatcher fileWatcher;
    private ApplicationLifecycle app;
    private LingoturkConfig lingoturkConfig;

    @Inject
    public ExperimentWatchServiceImplementation(ApplicationLifecycle app, LingoturkConfig lingoturkConfig) {
        this.app = app;
        this.lingoturkConfig = lingoturkConfig;

        fileWatcher = new FileWatcher(this.lingoturkConfig);

        this.app.addStopHook(() -> {
            fileWatcher.stop();
            return CompletableFuture.completedFuture(null);
        });

        Thread t = new Thread(fileWatcher);
        t.start();
    }

    @Override
    public boolean addExperimentType(String experimentType) {
        return fileWatcher.registerExperiment(experimentType);
    }

    @Override
    public boolean removeExperimentType(String experimentType) {
        fileWatcher.unregisterExperiment(experimentType);
        return true;
    }
}
