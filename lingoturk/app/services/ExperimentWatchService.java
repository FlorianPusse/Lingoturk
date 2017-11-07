package services;

import com.google.inject.ImplementedBy;

@ImplementedBy(ExperimentWatchServiceImplementation.class)
public interface ExperimentWatchService {
    public boolean addExperimentType(String experimentType);

    public boolean removeExperimentType(String experimentType);
}
