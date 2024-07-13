package software.coley.recaf.launcher.commands;

import org.slf4j.Logger;
import picocli.CommandLine.Command;
import software.coley.recaf.launcher.info.JavaFxVersion;
import software.coley.recaf.launcher.task.ExecutionTasks;
import software.coley.recaf.launcher.task.JavaFxTasks;
import software.coley.recaf.launcher.task.RecafTasks;
import software.coley.recaf.launcher.task.VersionUpdateResult;
import software.coley.recaf.launcher.util.Loggers;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Command for checking compatibility.
 */
@Command(name = "auto", description = {
		"Runs the suggested commands in order: ",
		" - compatibility",
		" - update-jfx -maxc 30 -maxs 60000000 -k",
		" - update-snapshot",
		" - run",
		"If one of the commands fails, the following ones are skipped."
})
public class Auto implements Callable<Void> {
	private static final Logger logger = Loggers.newLogger();

	@Override
	public Void call() {
		// Ensure compatibility
		if (!Compatibility.isCompatible(false, false))
			return null;

		// Update JavaFX when possible, clearing outdated cache entries when it gets too cluttered
		int jfxRuntimeVersion = JavaFxTasks.detectClasspathVersion();
		if (jfxRuntimeVersion <= 0) {
			JavaFxTasks.checkClearCache(false, true, 30, 64_000_000);
			if (JavaFxTasks.update(false) == null)
				return null;
		} else if (jfxRuntimeVersion < JavaFxVersion.MIN_SUGGESTED) {
			logger.warn("The current JDK bundles JavaFX {} which is less than the minimum suggested version of JavaFX {}",
					jfxRuntimeVersion, JavaFxVersion.MIN_SUGGESTED);
		}

		// Update Recaf.
		// TODO: When released, replace with - UpdateRecaf.update(true);
		VersionUpdateResult result = RecafTasks.updateFromSnapshot("dev4");
		if (result.getError() != null) {
			logger.error("Encountered error updating Recaf from latest snapshot", result.getError());
			return null;
		}

		// Run recaf.
		try {
			ExecutionTasks.run(true, null);
		} catch (IOException ex) {
			logger.error("Encountered error running Recaf", ex);
		}
		return null;
	}
}
