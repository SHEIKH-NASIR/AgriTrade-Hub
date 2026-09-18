package com.softpro.ATH.ai;

/**
 * Result returned by the AI demand forecasting engine.
 */
public class AIDemandPrediction {

    private final long predictedDemand;
    private final int confidence;
    private final String modelName;

    public AIDemandPrediction(
            long predictedDemand,
            int confidence,
            String modelName) {

        this.predictedDemand = predictedDemand;
        this.confidence = confidence;
        this.modelName = modelName;
    }

    public long getPredictedDemand() {
        return predictedDemand;
    }

    public int getConfidence() {
        return confidence;
    }

    public String getModelName() {
        return modelName;
    }
}