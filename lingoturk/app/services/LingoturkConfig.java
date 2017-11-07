package services;

import com.google.inject.ImplementedBy;

import java.io.IOException;
import java.util.List;

@ImplementedBy(LingoturkConfigImplementation.class)
public interface LingoturkConfig {

    String getPathPrefix();

    boolean useBackup();

    void setStaticIp(String ip) throws IOException;

    String getStaticIp();

    List<String> getExperimentNames();

}
