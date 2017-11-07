package services;

import play.inject.ApplicationLifecycle;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Module that ensures that a database backup is created on stop of the application.
 */
public class DatabaseModuleService {
    private final DatabaseService databaseService;

    @Inject
    public DatabaseModuleService(ApplicationLifecycle app, DatabaseService databaseService) {
        this.databaseService = databaseService;

        System.out.println("[info] play - ApplicationController has started...");

        Connection connection = this.databaseService.getConnection();

        app.addStopHook(() -> {
            System.out.println("[info] play - ApplicationController shutdown... Database backup created.");
            try {
                // If data is stored, back it up
                if (databaseService.isDatabaseCreated(connection) && databaseService.countExperiments() > 0) {
                    databaseService.backupDatabase();
                    System.out.println("[info] play - ApplicationController shutdown... Database backup created.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("[error] play - ApplicationController shutdown... Could not create Database backup.");
            }
            databaseService.closeConnection();
            return CompletableFuture.completedFuture(null);
        });
    }
}
