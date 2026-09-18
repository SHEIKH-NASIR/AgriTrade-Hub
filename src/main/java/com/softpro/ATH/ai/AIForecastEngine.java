package com.softpro.ATH.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * AI Demand Forecasting Engine.
 *
 * Hybrid forecasting architecture:
 *
 * 1. Enough history
 *      → Regularized ML regression
 *
 * 2. Moderate history
 *      → ML + historical trend
 *
 * 3. Limited history
 *      → Weighted historical demand
 *
 * 4. No history
 *      → No artificial demand is generated
 *
 * This engine is shared by both:
 *
 *      Independent Farmer
 *              +
 *             FPO
 */
@Service
public class AIForecastEngine {

    private static final String ML_MODEL =
            "ATH AI Demand Regression v2";

    private static final String HYBRID_MODEL =
            "ATH Hybrid ML + Historical Trend";

    private static final String BASELINE_MODEL =
            "ATH Historical Demand Baseline";

    private static final String NO_HISTORY_MODEL =
            "ATH Insufficient Demand History";


    /*
     * Number of historical training samples required
     * before trusting the ML regression model strongly.
     */
    private static final int MIN_ML_SAMPLES = 8;


    /*
     * Minimum number of actual orders required to
     * calculate a useful historical baseline.
     */
    private static final int MIN_BASELINE_ORDERS = 2;


    /**
     * Main prediction method.
     */
    public AIDemandPrediction predict(
            List<ForecastFeature> trainingSamples,
            ForecastFeature currentFeature) {

        if (currentFeature == null) {

            return new AIDemandPrediction(
                    0,
                    0,
                    NO_HISTORY_MODEL
            );
        }

        if (trainingSamples == null) {
            trainingSamples = List.of();
        }


        /*
         * --------------------------------------------------
         * CASE 1
         *
         * Enough historical ML samples.
         * --------------------------------------------------
         */
        if (trainingSamples.size()
                >= MIN_ML_SAMPLES) {

            AIDemandPrediction ml =
                    mlPrediction(
                            trainingSamples,
                            currentFeature
                    );

            /*
             * If ML generated a valid result, return it.
             */
            if (ml.getPredictedDemand() >= 0) {
                return ml;
            }
        }


        /*
         * --------------------------------------------------
         * CASE 2
         *
         * Some historical samples exist.
         *
         * Use a hybrid statistical forecast.
         * --------------------------------------------------
         */
        if (hasMeaningfulDemand(currentFeature)) {

            long baseline =
                    historicalBaseline(
                            currentFeature
                    );

            long trendAdjusted =
                    trendAdjustedForecast(
                            currentFeature,
                            baseline
                    );

            int confidence =
                    calculateBaselineConfidence(
                            currentFeature,
                            trainingSamples
                    );

            return new AIDemandPrediction(
                    trendAdjusted,
                    confidence,
                    trainingSamples.isEmpty()
                            ? BASELINE_MODEL
                            : HYBRID_MODEL
            );
        }


        /*
         * --------------------------------------------------
         * CASE 3
         *
         * No useful demand history.
         *
         * DO NOT invent a demand number.
         * --------------------------------------------------
         */
        return new AIDemandPrediction(
                0,
                0,
                NO_HISTORY_MODEL
        );
    }


    /**
     * Machine-learning prediction.
     */
    private AIDemandPrediction mlPrediction(
            List<ForecastFeature> samples,
            ForecastFeature currentFeature) {

        try {

            double[][] x =
                    new double[samples.size()][];

            double[] y =
                    new double[samples.size()];

            double[] scales =
                    calculateScales(samples);


            for (int i = 0;
                 i < samples.size();
                 i++) {

                ForecastFeature sample =
                        samples.get(i);

                x[i] =
                        normalize(
                                sample.toVector(),
                                scales
                        );

                y[i] =
                        Math.max(
                                0,
                                sample.getTargetDemand()
                        );
            }


            /*
             * Train regularized linear regression.
             */
            double[] coefficients =
                    trainRegression(
                            x,
                            y
                    );


            double[] current =
                    normalize(
                            currentFeature.toVector(),
                            scales
                    );


            /*
             * Calculate ML prediction.
             */
            double prediction =
                    coefficients[0];


            for (int j = 0;
                 j < current.length;
                 j++) {

                prediction +=
                        coefficients[j + 1]
                                * current[j];
            }


            if (Double.isNaN(prediction)
                    || Double.isInfinite(prediction)) {

                return new AIDemandPrediction(
                        -1,
                        0,
                        ML_MODEL
                );
            }


            prediction =
                    Math.max(
                            0,
                            prediction
                    );


            /*
             * Recent demand baseline.
             *
             * We blend 75% ML + 25% recent demand
             * so that abnormal regression outputs don't
             * completely dominate the forecast.
             */
            double recentBaseline =
                    Math.max(
                            currentFeature
                                    .getRecent30DayDemand(),

                            currentFeature
                                    .getPrevious30DayDemand()
                    );


            double finalPrediction;


            if (recentBaseline > 0) {

                finalPrediction =
                        prediction * 0.75
                                + recentBaseline * 0.25;

            } else {

                finalPrediction =
                        prediction;
            }


            long rounded =
                    Math.max(
                            0,
                            Math.round(
                                    finalPrediction
                            )
                    );


            int confidence =
                    calculateMLConfidence(
                            samples,
                            coefficients,
                            scales
                    );


            return new AIDemandPrediction(
                    rounded,
                    confidence,
                    ML_MODEL
            );

        } catch (Exception e) {

            /*
             * Never allow forecasting to break
             * the Farmer/FPO dashboard.
             */
            return new AIDemandPrediction(
                    -1,
                    0,
                    ML_MODEL
            );
        }
    }


    /**
     * Historical 30-day baseline.
     *
     * Recent demand gets the highest weight.
     */
    private long historicalBaseline(
            ForecastFeature feature) {

        double recent30 =
                feature.getRecent30DayDemand();

        double previous30 =
                feature.getPrevious30DayDemand();

        double previous60 =
                feature.getPrevious60DayDemand();


        /*
         * Recent 30 days:
         * 55%
         *
         * Previous 30 days:
         * 30%
         *
         * Older 30 days:
         * 15%
         */
        double forecast =
                recent30 * 0.55
                        + previous30 * 0.30
                        + previous60 * 0.15;


        /*
         * If only recent demand exists,
         * don't reduce it unnecessarily.
         */
        if (recent30 > 0
                && previous30 == 0
                && previous60 == 0) {

            forecast =
                    recent30;
        }


        return Math.max(
                0,
                Math.round(forecast)
        );
    }


    /**
     * Applies demand trend to the baseline.
     */
    private long trendAdjustedForecast(
            ForecastFeature feature,
            long baseline) {

        double recent =
                feature.getRecent30DayDemand();

        double previous =
                feature.getPrevious30DayDemand();


        if (baseline <= 0) {
            return 0;
        }


        /*
         * No previous demand.
         */
        if (previous <= 0) {
            return baseline;
        }


        double change =
                (recent - previous)
                        / previous;


        /*
         * Limit trend influence.
         *
         * This prevents a single abnormal period from
         * creating a ridiculous prediction.
         */
        change =
                Math.max(
                        -0.30,
                        Math.min(
                                0.30,
                                change
                        )
                );


        double adjusted =
                baseline
                        * (1.0 + change * 0.50);


        return Math.max(
                0,
                Math.round(adjusted)
        );
    }


    /**
     * Determines whether current demand information
     * is actually meaningful.
     */
    private boolean hasMeaningfulDemand(
            ForecastFeature feature) {

        return feature.getRecent7DayDemand() > 0
                || feature.getRecent30DayDemand() > 0
                || feature.getPrevious30DayDemand() > 0
                || feature.getPrevious60DayDemand() > 0;
    }


    /**
     * Calculates confidence for historical fallback.
     */
    private int calculateBaselineConfidence(
            ForecastFeature feature,
            List<ForecastFeature> samples) {

        int confidence = 35;


        /*
         * Recent demand available.
         */
        if (feature.getRecent30DayDemand() > 0) {
            confidence += 10;
        }


        /*
         * Previous demand available.
         */
        if (feature.getPrevious30DayDemand() > 0) {
            confidence += 10;
        }


        /*
         * Older demand available.
         */
        if (feature.getPrevious60DayDemand() > 0) {
            confidence += 5;
        }


        /*
         * More training windows → more confidence.
         */
        if (samples != null) {

            if (samples.size() >= 3) {
                confidence += 5;
            }

            if (samples.size() >= 5) {
                confidence += 5;
            }
        }


        return Math.min(
                70,
                confidence
        );
    }


    /**
     * Calculates ML confidence.
     */
    private int calculateMLConfidence(
            List<ForecastFeature> samples,
            double[] coefficients,
            double[] scales) {

        if (samples == null
                || samples.isEmpty()) {

            return 0;
        }


        double totalActual = 0;
        double totalError = 0;


        for (ForecastFeature sample :
                samples) {

            double[] input =
                    normalize(
                            sample.toVector(),
                            scales
                    );


            double prediction =
                    coefficients[0];


            for (int j = 0;
                 j < input.length;
                 j++) {

                prediction +=
                        coefficients[j + 1]
                                * input[j];
            }


            prediction =
                    Math.max(
                            0,
                            prediction
                    );


            double actual =
                    Math.max(
                            0,
                            sample.getTargetDemand()
                    );


            totalActual += actual;

            totalError +=
                    Math.abs(
                            actual - prediction
                    );
        }


        if (totalActual <= 0) {
            return 50;
        }


        double meanActual =
                totalActual
                        / samples.size();


        double meanAbsoluteError =
                totalError
                        / samples.size();


        double accuracy =
                1.0
                        -
                        (
                                meanAbsoluteError
                                        /
                                        Math.max(
                                                meanActual,
                                                1.0
                                        )
                        );


        int confidence =
                (int) Math.round(
                        accuracy * 100
                );


        /*
         * Confidence should never claim unrealistic
         * certainty.
         */
        confidence =
                Math.max(
                        50,
                        Math.min(
                                95,
                                confidence
                        )
                );


        /*
         * More historical windows improve reliability.
         */
        if (samples.size() >= 20) {

            confidence =
                    Math.min(
                            95,
                            confidence + 3
                    );
        }


        return confidence;
    }


    /**
     * Calculates normalization scales.
     */
    private double[] calculateScales(
            List<ForecastFeature> samples) {

        if (samples == null
                || samples.isEmpty()) {

            return new double[0];
        }


        int featureCount =
                samples.get(0)
                        .toVector()
                        .length;


        double[] scales =
                new double[featureCount];


        Arrays.fill(
                scales,
                1.0
        );


        for (ForecastFeature sample :
                samples) {

            double[] vector =
                    sample.toVector();


            for (int i = 0;
                 i < vector.length;
                 i++) {

                scales[i] =
                        Math.max(
                                scales[i],
                                Math.abs(
                                        vector[i]
                                )
                        );
            }
        }


        return scales;
    }


    /**
     * Normalizes feature values.
     */
    private double[] normalize(
            double[] vector,
            double[] scales) {

        double[] result =
                new double[vector.length];


        for (int i = 0;
             i < vector.length;
             i++) {

            result[i] =
                    vector[i]
                            /
                            Math.max(
                                    scales[i],
                                    1.0
                            );
        }


        return result;
    }


    /**
     * Ridge regression.
     *
     * beta =
     * (X'X + lambda I)^-1 X'Y
     */
    private double[] trainRegression(
            double[][] x,
            double[] y) {

        int rows =
                x.length;

        int features =
                x[0].length;

        int dimension =
                features + 1;


        double[][] matrix =
                new double[
                        dimension
                ][
                        dimension
                ];


        double[] vector =
                new double[
                        dimension
                ];


        double lambda =
                0.01;


        for (int i = 0;
             i < rows;
             i++) {

            double[] augmented =
                    new double[
                            dimension
                    ];


            augmented[0] =
                    1.0;


            System.arraycopy(
                    x[i],
                    0,
                    augmented,
                    1,
                    features
            );


            for (int r = 0;
                 r < dimension;
                 r++) {

                vector[r] +=
                        augmented[r]
                                * y[i];


                for (int c = 0;
                     c < dimension;
                     c++) {

                    matrix[r][c] +=
                            augmented[r]
                                    * augmented[c];
                }
            }
        }


        /*
         * Regularization.
         *
         * Don't regularize the intercept.
         */
        for (int i = 1;
             i < dimension;
             i++) {

            matrix[i][i] +=
                    lambda;
        }


        return solveLinearSystem(
                matrix,
                vector
        );
    }


    /**
     * Gaussian elimination with partial pivoting.
     */
    private double[] solveLinearSystem(
            double[][] matrix,
            double[] vector) {

        int n =
                vector.length;


        double[][] augmented =
                new double[
                        n
                ][
                        n + 1
                ];


        for (int i = 0;
             i < n;
             i++) {

            System.arraycopy(
                    matrix[i],
                    0,
                    augmented[i],
                    0,
                    n
            );


            augmented[i][n] =
                    vector[i];
        }


        for (int column = 0;
             column < n;
             column++) {

            int pivot =
                    column;


            for (int row = column + 1;
                 row < n;
                 row++) {

                if (Math.abs(
                        augmented[row][column])
                        >
                        Math.abs(
                                augmented[pivot][column])) {

                    pivot =
                            row;
                }
            }


            double[] temp =
                    augmented[column];


            augmented[column] =
                    augmented[pivot];


            augmented[pivot] =
                    temp;


            if (Math.abs(
                    augmented[column][column])
                    < 1e-10) {

                continue;
            }


            double divisor =
                    augmented[column][column];


            for (int j = column;
                 j <= n;
                 j++) {

                augmented[column][j] /=
                        divisor;
            }


            for (int row = 0;
                 row < n;
                 row++) {

                if (row == column) {
                    continue;
                }


                double factor =
                        augmented[row][column];


                for (int j = column;
                     j <= n;
                     j++) {

                    augmented[row][j] -=
                            factor
                                    * augmented[column][j];
                }
            }
        }


        double[] solution =
                new double[n];


        for (int i = 0;
             i < n;
             i++) {

            solution[i] =
                    augmented[i][n];


            if (Double.isNaN(
                    solution[i])
                    ||
                    Double.isInfinite(
                            solution[i])) {

                solution[i] =
                        0;
            }
        }


        return solution;
    }
}