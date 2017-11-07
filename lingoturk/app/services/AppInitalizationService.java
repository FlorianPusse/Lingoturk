package services;

import akka.actor.ActorSystem;
import models.User;
import play.db.ebean.EbeanConfig;
import play.inject.ApplicationLifecycle;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Module that handles functionality that must be run on startup, such as
 * checking if the database needs to be restored, the default user must be created
 * and to start the background service.
 */
public class AppInitalizationService {

    private final DatabaseService databaseService;
    private final DatabaseModuleService databaseModule;
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;
    private final EbeanConfig ebeanConfig;
    private final LingoturkConfig lingoturkConfig;

    @Inject
    public AppInitalizationService(ApplicationLifecycle app, DatabaseService databaseService, DatabaseModuleService databaseModule, ActorSystem actorSystem, ExecutionContext executionContext, EbeanConfig ebeanConfig, LingoturkConfig lingoturkConfig) {
        this.databaseService = databaseService;
        this.databaseModule = databaseModule;
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.ebeanConfig = ebeanConfig;
        this.lingoturkConfig = lingoturkConfig;

        Connection connection = this.databaseService.getConnection();
        if (lingoturkConfig.useBackup()) {
            try {
                if (databaseService.isDatabaseCreated(connection) && databaseService.countExperiments() == 0) {
                    File backupFile = new File(this.lingoturkConfig.getPathPrefix() + "backup/backup.sql");

                    if (backupFile.exists()) {
                        System.out.println("[info] play - Load backup file.");

                        databaseService.restoreDatabase(backupFile);

                        System.out.println("[info] play - Backup imported successfully.");
                    }
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }

        // check if at least one user is present in the database. otherwise fall back to default
        if (!User.existsAny()) {
            System.out.println("[info] play - No existing user. Fall back to default user.");
            User u = new User("admin", "admin");
            u.save();
        }

        System.out.println("[info] play - Started background service...");
        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.MILLISECONDS),
                Duration.create(1, TimeUnit.MINUTES),
                new AsynchronousJob(this.databaseService),
                this.executionContext
        );
    }
}