/*
 * Copyright (c) 2016.
 *
 * This file is part of Project AGI. <http://agi.io>
 *
 * Project AGI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Project AGI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Project AGI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.agi.core.alg;

import io.agi.core.ann.unsupervised.*;
import io.agi.core.orm.ObjectMap;

import java.util.Random;

/**
 * Created by dave on 14/05/16.
 */
public class RegionLayerFactory {

    public RegionLayerConfig _rc;

    public RegionLayerFactory() {

    }

    public RegionLayer create(
            ObjectMap om,
            String regionName,
            Random random,

            // Feedforward input size
            int input1Width,
            int input1Height,

            int input2Width,
            int input2Height,

            // Feedback input size
            int feedbackWidthCells,
            int feedbackHeightCells,

            // Algorithm specific parameters
            // Region size
            int regionWidthColumns,
            int regionHeightColumns,

            // Column Sizing
            int classifierWidthCells,
            int classifierHeightCells,
            int classifierDepthCells,

            // Organizer training
            boolean organizerTrainOnChange,
            boolean emitUnchangedCells,
            float receptiveFieldsTrainingSamples,
            float defaultPredictionInhibition,
            int classifiersPerBit,
            float organizerNeighbourhoodRange,
//            float organizerLearningRate,
//            float organizerElasticity,
//            float organizerLearningRateNeighbours,
//            float organizerNoiseMagnitude,
//            int organizerEdgeMaxAge,
//            float organizerStressLearningRate,
//            float organizerStressThreshold,
//            int organizerGrowthInterval,

            // Classifier training
            float classifierLearningRate,
            float classifierLearningRateNeighbours,
            float classifierNoiseMagnitude,
            int classifierEdgeMaxAge,
            float classifierStressLearningRate,
            float classifierStressSplitLearningRate,
            float classifierStressThreshold,
            int classifierGrowthInterval,

            // Predictor
            float predictorLearningRate ) {

        RegionLayerConfig rc = new RegionLayerConfig();

        // Computed or fixed parameters
        int input1Area = input1Width * input1Height;
        int input2Area = input2Width * input2Height;
        int classifierInputs = input1Area + input2Area;
        int feedbackAreaCells = feedbackWidthCells * feedbackHeightCells;
        int organizerInputs = 2 * 2;

        ParameterLessSelfOrganizingMapConfig organizerConfig = new ParameterLessSelfOrganizingMapConfig();
        organizerConfig.setup(
                om, RegionLayerConfig.SUFFIX_ORGANIZER, random, // temp name
                organizerInputs, regionWidthColumns, regionHeightColumns,
                organizerNeighbourhoodRange );
//                organizerLearningRate, organizerElasticity );
//        GrowingNeuralGasConfig organizerConfig = new GrowingNeuralGasConfig();
//        organizerConfig.setup(
//                om, RegionLayerConfig.SUFFIX_ORGANIZER, random, // temp name
//                organizerInputs, regionWidthColumns, regionHeightColumns,
//                organizerLearningRate, organizerLearningRateNeighbours, organizerNoiseMagnitude,
//                organizerEdgeMaxAge, organizerStressLearningRate, organizerStressThreshold, organizerGrowthInterval );

        GrowingNeuralGasConfig classifierConfig = new GrowingNeuralGasConfig();
        classifierConfig.setup(
                om, RegionLayerConfig.SUFFIX_CLASSIFIER, random, // temp name
                classifierInputs, classifierWidthCells, classifierHeightCells,
                classifierLearningRate, classifierLearningRateNeighbours, classifierNoiseMagnitude,
                classifierEdgeMaxAge, classifierStressLearningRate, classifierStressSplitLearningRate, classifierStressThreshold, classifierGrowthInterval );

        rc.setup( om, regionName, random, organizerConfig, classifierConfig, input1Width, input1Height, input2Width, input2Height, feedbackAreaCells, predictorLearningRate, receptiveFieldsTrainingSamples, defaultPredictionInhibition, organizerTrainOnChange, emitUnchangedCells, classifiersPerBit, classifierDepthCells );

        this.setup( rc );

        RegionLayer regionLayer = this.createRegionLayer( regionName );
        return regionLayer;
    }

    public void setup( RegionLayerConfig rc ) {
        _rc = rc;
    }

    public RegionLayerConfig getRegionLayerConfig() {
        return _rc;
    }

    public RegionLayer createRegionLayer( String name ) {
        RegionLayer r = new RegionLayer( name, _rc._om );
        r.setup( this, _rc );
        return r;
    }

//    public GrowingNeuralGas createOrganizer( RegionLayer r ) {
//
//        String name = r.getKey( RegionLayerConfig.SUFFIX_ORGANIZER );
//        GrowingNeuralGasConfig c = new GrowingNeuralGasConfig();
//        c.copyFrom( _rc._organizerConfig, name );
//
//        GrowingNeuralGas gng = new GrowingNeuralGas( c._name, c._om );
//        gng.setup( c );
//
//        return gng;
//    }
//
//    public DynamicSelfOrganizingMap createOrganizer( RegionLayer r ) {
//
//        String name = r.getKey( RegionLayerConfig.SUFFIX_ORGANIZER );
//        DynamicSelfOrganizingMapConfig c = new DynamicSelfOrganizingMapConfig();
//        c.copyFrom( _rc._organizerConfig, name );
//
//        DynamicSelfOrganizingMap dsom = new DynamicSelfOrganizingMap( c._name, c._om );
//        dsom.setup( c );
//
//        return dsom;
//    }

    public ParameterLessSelfOrganizingMap createOrganizer( RegionLayer r ) {

        String name = r.getKey( RegionLayerConfig.SUFFIX_ORGANIZER );
        ParameterLessSelfOrganizingMapConfig c = new ParameterLessSelfOrganizingMapConfig();
        c.copyFrom( _rc._organizerConfig, name );

        ParameterLessSelfOrganizingMap som = new ParameterLessSelfOrganizingMap( c._name, c._om );
        som.setup( c );

        return som;
    }

    public GrowingNeuralGas createClassifier( RegionLayer r ) {

        String name = r.getKey( RegionLayerConfig.SUFFIX_CLASSIFIER );
        GrowingNeuralGasConfig c = new GrowingNeuralGasConfig();
        c.copyFrom( _rc._classifierConfig, name );

        GrowingNeuralGas gng = new GrowingNeuralGas( c._name, c._om );
        gng.setup( c );

        return gng;
    }
}
