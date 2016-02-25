package cloud.benchflow.driversmaker.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 22/02/16.
 */
public class Trial {

    @JsonProperty("benchmarkId")
    private String benchmarkId;

    @JsonProperty("experimentNumber")
    private int experimentNumber;

    @JsonProperty("trialNumber")
    private int trialNumber;

    @JsonProperty("totalTrials")
    private int totalTrials;


    public String getTrialId() {
        return benchmarkId + "." + experimentNumber + "." + trialNumber;
    }

    public String getExperimentId() {
        return benchmarkId + "." + experimentNumber;
    }

    public int getTrialNumber() {
        return trialNumber;
    }

    public void setTrialNumber(int trialNumber) {
        this.trialNumber = trialNumber;
    }

    public int getExperimentNumber() {
        return experimentNumber;
    }

    public void setExperimentNumber(int experimentNumber) {
        this.experimentNumber = experimentNumber;
    }

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public void setBenchmarkId(String benchmarkId) {
        this.benchmarkId = benchmarkId;
    }

    public int getTotalTrials() {
        return totalTrials;
    }

    public void setTotalTrials(int totalTrials) {
        this.totalTrials = totalTrials;
    }
}
