/*
 * Copyright (c) 2017.
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

package io.agi.framework.demo.papers;

import io.agi.core.orm.AbstractPair;
import io.agi.core.util.PropertiesUtil;
import io.agi.framework.Framework;
import io.agi.framework.Main;
import io.agi.framework.Node;
import io.agi.framework.demo.CreateEntityMain;
import io.agi.framework.demo.mnist.ImageLabelEntity;
import io.agi.framework.demo.mnist.ImageLabelEntityConfig;
import io.agi.framework.entities.*;
import io.agi.framework.entities.stdp.*;
import io.agi.framework.factories.CommonEntityFactory;
import io.agi.framework.persistence.models.ModelData;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by dave on 8/07/16.
 */
public class SpikingConvolutionalDemo extends CreateEntityMain {

    public static void main( String[] args ) {
        SpikingConvolutionalDemo demo = new SpikingConvolutionalDemo();
        demo.mainImpl(args );
    }

// - 2/  replace clear with decay of integrated values, so it discovers the time steps
// - 3/  add prediction to help classification
// - 4/  add predictive coding (larger spike train output on prediction FN error)

// TODO after this, make a version that uses predictive encoding via feedback, which both uses feedback to help recognize and draws resources towards errors
// TODO restore negative features
//- Prediction: Do we have a timing rule that input from the apical dendrite must arrive before a post-spike not after, AND that it must not
//- Do we implement the spike-train encoding? [Yes, because bio evidenc for it]. Binding problem - joint handling of dynamically allocated variables.
//- Feedback: What to do with feedback? Start with all zero weights? Do we train when prediction fails? [ New evidence: we have papers showing PC and feedback reduces time to output spike or suppresses/truncates output spike ]

    public void createEntities( Node n ) {

        // Dataset
//        String trainingPath = "/Users/gideon/Development/ProjectAGI/AGIEF/datasets/mnist/training-small";
//        String testingPath = "/Users/gideon/Development/ProjectAGI/AGIEF/datasets/mnist/training-small, /Users/gideon/Development/ProjectAGI/AGIEF/datasets/mnist/testing-small";

//        String trainingPath = "/home/dave/workspace/agi.io/data/mnist/1k_test";
//        String  testingPath = "/home/dave/workspace/agi.io/data/mnist/1k_test";

//        String trainingPath = "/home/dave/workspace/agi.io/data/mnist/10k_train";
//        String  testingPath = "/home/dave/workspace/agi.io/data/mnist/1k_test";

        String trainingPath = "/home/dave/workspace/agi.io/data/mnist/cycle10";
        String testingPath = "/home/dave/workspace/agi.io/data/mnist/cycle10,/home/dave/workspace/agi.io/data/mnist/cycle3";
//        String trainingPath = "/home/dave/workspace/agi.io/data/mnist/cycle3";
//        String testingPath = "/home/dave/workspace/agi.io/data/mnist/cycle3";

//        String trainingPath = "/Users/gideon/Development/ProjectAGI/AGIEF/datasets/mnist/training-small";
//        String testingPath = "/Users/gideon/Development/ProjectAGI/AGIEF/datasets/mnist/testing-small";

        // Parameters
//        int flushInterval = 20;
//        int flushInterval = -1; // never flush
//        String flushWriteFilePath = "/home/dave/workspace/agi.io/data/flush";
//        String flushWriteFilePrefixTruth = "flushedTruth";
//        String flushWriteFilePrefixFeatures = "flushedFeatures";

        boolean logDuringTraining = true;
        boolean debug = false;
//        boolean logDuringTraining = false;
        boolean cacheAllData = true;
        boolean terminateByAge = false;
        int terminationAge = 1000;//50000;//25000;
        int trainingEpochs = 2;//30;//20; // = 5 * 10 images * 30 repeats = 1500      30*10*30 =
        int testingEpochs = 1; // = 1 * 10 images * 30 repeats = 300
        int imageRepeats = 30; // paper - 30
//        int imagesPerEpoch = 10;

        // Entity names
        String experimentName           = Framework.GetEntityName( "experiment" );
        String imageLabelName           = Framework.GetEntityName( "image-class" );
        String vectorSeriesName         = Framework.GetEntityName( "feature-series" );
        String valueSeriesName          = Framework.GetEntityName( "label-series" );

        // Algorithm
        String dogPosName               = Framework.GetEntityName( "dog-pos" );
        String dogNegName               = Framework.GetEntityName( "dog-neg" );
        String spikeEncoderName         = Framework.GetEntityName( "spike-encoder" );
        String spikingConvolutionalName = Framework.GetEntityName( "stdp-cnn" );

        // Create entities
        String parentName = null;
        parentName = Framework.CreateEntity( experimentName, ExperimentEntity.ENTITY_TYPE, n.getName(), null ); // experiment is the root entity
        parentName = Framework.CreateEntity( imageLabelName, ImageLabelEntity.ENTITY_TYPE, n.getName(), parentName );

        parentName = Framework.CreateEntity( dogPosName, DifferenceOfGaussiansEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( dogNegName, DifferenceOfGaussiansEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( spikeEncoderName, ConvolutionalSpikeEncoderEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( spikingConvolutionalName, SpikingConvolutionalNetworkEntity.ENTITY_TYPE, n.getName(), parentName );

        parentName = Framework.CreateEntity( vectorSeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName ); // 2nd, class region updates after first to get its feedback
        parentName = Framework.CreateEntity( valueSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName ); // 2nd, class region updates after first to get its feedback

        // Connect the entities' data
        Framework.SetDataReference( dogPosName, DifferenceOfGaussiansEntity.DATA_INPUT, imageLabelName, ImageLabelEntity.OUTPUT_IMAGE );
        Framework.SetDataReference( dogNegName, DifferenceOfGaussiansEntity.DATA_INPUT, imageLabelName, ImageLabelEntity.OUTPUT_IMAGE );

        Framework.SetDataReference( spikeEncoderName, ConvolutionalSpikeEncoderEntity.DATA_INPUT_POS, dogPosName, DifferenceOfGaussiansEntity.DATA_OUTPUT );
//        Framework.SetDataReference( spikeEncoderName, ConvolutionalSpikeEncoderEntity.DATA_INPUT_NEG, dogNegName, DifferenceOfGaussiansEntity.DATA_OUTPUT );

        // a) Image to image region, and decode
        Framework.SetDataReference( spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_INPUT, spikeEncoderName, ConvolutionalSpikeEncoderEntity.DATA_OUTPUT );

        ArrayList< AbstractPair< String, String > > featureDatas = new ArrayList<>();
        featureDatas.add( new AbstractPair<>( spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_OUTPUT ) );
        Framework.SetDataReferences( vectorSeriesName, VectorSeriesEntity.INPUT, featureDatas ); // get current state from the region to be used to predict

        // Experiment config
        if( !terminateByAge ) {
            Framework.SetConfig( experimentName, "terminationEntityName", imageLabelName );
            Framework.SetConfig( experimentName, "terminationConfigPath", "terminate" );
            Framework.SetConfig( experimentName, "terminationAge", "-1" ); // wait for mnist to decide
        }
        else {
            Framework.SetConfig( experimentName, "terminationAge", String.valueOf( terminationAge ) ); // fixed steps
        }

        float stdDev1 = 1f;
        float stdDev2 = 2f;
        int kernelSize = 7;
        SetDoGEntityConfig( dogPosName, stdDev1, stdDev2, kernelSize, 1.0f );
        SetDoGEntityConfig( dogNegName, stdDev2, stdDev1, kernelSize, 1.0f );

        float spikeThreshold = 5.0f;
        String clearFlagEntityName = imageLabelName;
        String clearFlagConfigPath = "imageChanged";
        SetSpikeEncoderEntityConfig( spikeEncoderName, spikeThreshold, clearFlagEntityName, clearFlagConfigPath );

        // cache all data for speed, when enabled
        Framework.SetConfig( experimentName, "cache", String.valueOf( cacheAllData ) );
        Framework.SetConfig( imageLabelName, "cache", String.valueOf( cacheAllData ) );
        Framework.SetConfig( spikeEncoderName, "cache", String.valueOf( cacheAllData ) );
        Framework.SetConfig( spikingConvolutionalName, "cache", String.valueOf( cacheAllData ) );
        Framework.SetConfig( vectorSeriesName, "cache", String.valueOf( cacheAllData ) );
        Framework.SetConfig( valueSeriesName, "cache", String.valueOf( cacheAllData ) );


        // MNIST config
        String trainingEntities = spikingConvolutionalName;
        String testingEntities = "";
        if( logDuringTraining ) {
            trainingEntities += "," + vectorSeriesName + "," + valueSeriesName;
        }
        testingEntities = vectorSeriesName + "," + valueSeriesName;
        SetImageLabelEntityConfig( imageLabelName, trainingPath, testingPath, trainingEpochs, testingEpochs, imageRepeats, trainingEntities, testingEntities );


        // Algorithm config
        int inputWidth = 28;
        int inputHeight = 28;
//        int inputDepth = 2;
        int inputDepth = 1;

        SetSpikingConvolutionalEntityConfig(
                spikingConvolutionalName, clearFlagEntityName, clearFlagConfigPath,
                inputWidth, inputHeight, inputDepth,
                imageRepeats );


        // LOGGING config
        // NOTE about logging: We accumulate the labels and features for all images, but then we only append a new sample of (features,label) every N steps
        // This timing corresponds with the change from one image to another. In essence we allow the network to respond to the image for a few steps, while recording its output
        int accumulatePeriod = imageRepeats;
        int period = -1;
        VectorSeriesEntityConfig.Set( vectorSeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );

        // Log image label for each set of features
        String valueSeriesInputEntityName = imageLabelName;
        String valueSeriesInputConfigPath = "imageLabel";
        String valueSeriesInputDataName = "";
        int inputDataOffset = 0;
        float accumulateFactor = 1f / imageRepeats;
        ValueSeriesEntityConfig.Set( valueSeriesName, accumulatePeriod, accumulateFactor, -1, period, valueSeriesInputEntityName, valueSeriesInputConfigPath, valueSeriesInputDataName, inputDataOffset );
        // LOGGING


        // Debug the algorithm
        if( debug == false ) {
            return; // we're done
        }

        period = 30 * 6;
        accumulatePeriod = 1;

        int controllerPeriod = -1;
        parentName = CreateControllerLogs( n, parentName, spikingConvolutionalName, accumulatePeriod, controllerPeriod );

        String encSeriesName = Framework.GetEntityName( "enc-series" );
        parentName = Framework.CreateEntity( encSeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        Framework.SetDataReference( encSeriesName, VectorSeriesEntity.INPUT, spikeEncoderName, ConvolutionalSpikeEncoderEntity.DATA_OUTPUT );
        VectorSeriesEntityConfig.Set( encSeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );

        String netInh1SeriesName = Framework.GetEntityName( "net-inh-1-series" );
        String netInt1SeriesName = Framework.GetEntityName( "net-int-1-series" );
        String netSpk1SeriesName = Framework.GetEntityName( "net-spk-1-series" );

        String netInh2SeriesName = Framework.GetEntityName( "net-inh-2-series" );
        String netInt2SeriesName = Framework.GetEntityName( "net-int-2-series" );
        String netSpk2SeriesName = Framework.GetEntityName( "net-spk-2-series" );

        parentName = Framework.CreateEntity( netInh1SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( netInt1SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( netSpk1SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );

        parentName = Framework.CreateEntity( netInh2SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( netInt2SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        parentName = Framework.CreateEntity( netSpk2SeriesName, VectorSeriesEntity.ENTITY_TYPE, n.getName(), parentName );

        String layer = "0";
        Framework.SetDataReference( netInh1SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_POOL_INHIBITION_ + layer );
        Framework.SetDataReference( netInt1SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_CONV_INTEGRATED_ + layer );
//        Framework.SetDataReference( netSpk1SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_CONV_SPIKES_ + layer );
        Framework.SetDataReference( netSpk1SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_POOL_SPIKES_INTEGRATED_ + layer );

        layer = "1";
//        Framework.SetDataReference( netInh2SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_CONV_INHIBITION_ + layer );
        Framework.SetDataReference( netInh2SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_POOL_INHIBITION_ + layer );
        Framework.SetDataReference( netInt2SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_CONV_INTEGRATED_ + layer );
//        Framework.SetDataReference( netSpk2SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_CONV_SPIKES_ + layer );
        Framework.SetDataReference( netSpk2SeriesName, VectorSeriesEntity.INPUT, spikingConvolutionalName, SpikingConvolutionalNetworkEntity.DATA_LAYER_POOL_SPIKES_INTEGRATED_ + layer );

        VectorSeriesEntityConfig.Set( netInh1SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );
        VectorSeriesEntityConfig.Set( netInt1SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );
        VectorSeriesEntityConfig.Set( netSpk1SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );

        VectorSeriesEntityConfig.Set( netInh2SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );
        VectorSeriesEntityConfig.Set( netInt2SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );
        VectorSeriesEntityConfig.Set( netSpk2SeriesName, accumulatePeriod, period, ModelData.ENCODING_SPARSE_REAL );
    }

    protected static String CreateControllerLogs( Node n, String parentName, String spikingConvolutionalName, int accumulatePeriod, int controllerPeriod ) {

        String controllerInputSeriesName = Framework.GetEntityName( "controller-input" );
        parentName = Framework.CreateEntity( controllerInputSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerInputSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerInput", null, 0 );

        String controllerInputAccumulatedSeriesName = Framework.GetEntityName( "controller-input-accumulated" );
        parentName = Framework.CreateEntity( controllerInputAccumulatedSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerInputAccumulatedSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerInputAccumulated", null, 0 );

        String controllerErrIntSeriesName = Framework.GetEntityName( "controller-error-integrated" );
        parentName = Framework.CreateEntity( controllerErrIntSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerErrIntSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerErrorIntegrated", null, 0 );

        String controllerOutputSeriesName = Framework.GetEntityName( "controller-output" );
        parentName = Framework.CreateEntity( controllerOutputSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerOutputSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerOutput", null, 0 );

        String controllerErrorSeriesName = Framework.GetEntityName( "controller-error" );
        parentName = Framework.CreateEntity( controllerErrorSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerErrorSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerError", null, 0 );

        String controllerThresholdSeriesName = Framework.GetEntityName( "controller-threshold" );
        parentName = Framework.CreateEntity( controllerThresholdSeriesName, ValueSeriesEntity.ENTITY_TYPE, n.getName(), parentName );
        ValueSeriesEntityConfig.Set( controllerThresholdSeriesName, accumulatePeriod, 1.f, -1, controllerPeriod, spikingConvolutionalName, "controllerThreshold", null, 0 );

        return parentName;
    }

    protected static void SetDoGEntityConfig( String entityName, float stdDev1, float stdDev2, int kernelSize, float scaling ) {
        DifferenceOfGaussiansEntityConfig entityConfig = new DifferenceOfGaussiansEntityConfig();
        entityConfig.cache = true;
        entityConfig.kernelWidth = kernelSize;
        entityConfig.kernelHeight = entityConfig.kernelWidth;
        entityConfig.stdDev1 = stdDev1;
        entityConfig.stdDev2 = stdDev2;
        entityConfig.scaling = scaling;
        entityConfig.min = -2.0f;
        entityConfig.max = 2.0f;

        Framework.SetConfig( entityName, entityConfig );
    }

    protected static void SetImageLabelEntityConfig( String entityName, String trainingPath, String testingPath, int trainingEpochs, int testingEpochs, int repeats, String trainingEntities, String testingEntities ) {

        ImageLabelEntityConfig entityConfig = new ImageLabelEntityConfig();
        entityConfig.cache = true;
        entityConfig.receptiveField.receptiveFieldX = 0;
        entityConfig.receptiveField.receptiveFieldY = 0;
        entityConfig.receptiveField.receptiveFieldW = 28;
        entityConfig.receptiveField.receptiveFieldH = 28;
        entityConfig.resolution.resolutionX = 28;
        entityConfig.resolution.resolutionY = 28;

        entityConfig.greyscale = true;
        entityConfig.invert = true;
//        entityConfig.sourceType = BufferedImageSourceFactory.TYPE_IMAGE_FILES;
//        entityConfig.sourceFilesPrefix = "postproc";
        entityConfig.sourceFilesPathTraining = trainingPath;
        entityConfig.sourceFilesPathTesting = testingPath;
        entityConfig.trainingEpochs = trainingEpochs;
        entityConfig.testingEpochs = testingEpochs;
        entityConfig.trainingEntities = trainingEntities;
        entityConfig.testingEntities = testingEntities;
        entityConfig.resolution.resolutionY = 28;

        entityConfig.shuffleTraining = false;
        entityConfig.imageRepeats = repeats;

        Framework.SetConfig( entityName, entityConfig );
    }

    protected static void SetSpikeEncoderEntityConfig( String entityName, float spikeThreshold, String clearFlagEntityName, String clearFlagConfigPath ) {

        ConvolutionalSpikeEncoderEntityConfig entityConfig = new ConvolutionalSpikeEncoderEntityConfig();
        entityConfig.cache = true;
        entityConfig.spikeThreshold = spikeThreshold;
        entityConfig.clearFlagEntityName = clearFlagEntityName;
        entityConfig.clearFlagConfigPath = clearFlagConfigPath;

        Framework.SetConfig( entityName, entityConfig );
    }

    protected static void SetSpikingConvolutionalEntityConfig(
            String entityName,
            String clearFlagEntityName,
            String clearFlagConfigPath,
            int inputWidth,
            int inputHeight,
            int inputDepth,
            int imageRepeats ) {

        SpikingConvolutionalNetworkEntityConfig entityConfig = new SpikingConvolutionalNetworkEntityConfig();

        entityConfig.cache = true;

        // This flag is used to clear inhibition and accumulated potential on new image.
        entityConfig.clearFlagEntityName = clearFlagEntityName;
        entityConfig.clearFlagConfigPath = clearFlagConfigPath;

        // "Synaptic weights of convolutional neurons initiate with random values drown from a normal distribution with the mean of 0.8 and STD of 0.05"
        entityConfig.kernelWeightsStdDev = 0.05f;
        entityConfig.kernelWeightsMean = 0.8f;
        entityConfig.kernelWeightsLearningRate = 0.01f;
//        entityConfig.learningRatePos = 0.0001f;//4f; // from paper
//        entityConfig.learningRateNeg = 0.0003f; // from paper

        // Note on configuring spike frequencies
        // Let's say we want a spike density of K=5% per image.
        // That means if we have 100 cells, then we will have 5 spikes.
        // Since we have N=30 repeats, to get those 5 spikes we actually need a per-update frequency of
        // K/N = 0.05 / 30 = 0.001666667 spikes per update.
        // This is the target frequency
        //maybe I wanna make them fire more easily, but suffer from inhibition to sparsen? Yes (current thinking)

        entityConfig.nbrLayers = 2;//3;

        int iw = inputWidth;
        int ih = inputHeight;
        int id = inputDepth;

        int[] layerDepths = { 28,10 }; // reduce for speed
//        int[] layerPoolingSize = { 2,8 }; // for classification in Z
        int[] layerPoolingSize = { 2,2 }; // for reconstruction, reduce pooling in 2nd layer
        int[] layerFieldSize = { 5,5 };
        int[] layerInputPaddings = { 0,0 };

        // Generate config properties from these values:
        for( int layer = 0; layer < entityConfig.nbrLayers; ++layer ) {

            // Geometry of layer
            String prefix = "";
            if( layer > 0 ) prefix = ",";

            int layerInputPadding = layerInputPaddings[ layer ];
            int layerInputStride = 1;//layerInputStrides[ layer ];
            int ld = layerDepths[ layer ];
            int pw = layerPoolingSize[ layer ];
            int ph = pw;
            int fw = layerFieldSize[ layer ];
            int fh = fw;
            int fd = id;
            int lw = iw - fw +1;//layerWidths[ layer ];;
            int lh = ih - fh +1;//layerHeights[ layer ];;

            // Kernel training parameters
            // Duty cycle per kernel. The rate is averaged over the whole area in X and Y. What we want to define is how
            // often the kernels are used across Z and T (time). This also depends on how many models we have.
            // Also it only really matters the relative gain for each kernel, because the threshold adapts on a per-layer
            // basis to control the spike density.
            float timeScaling = 1.f / (float)imageRepeats; // i.e. 1 if 1 repeat per image, or 0.5 if 2 repeats/image
            float zScaling = 1.f / (float)ld; // uniform
            float layerKernelSpikeFrequencyLearningRate = 0.0001f; // Learn very slowly
            float layerKernelSpikeFrequencyTarget = zScaling * timeScaling; // how often each kernel should fire, as a measure of average density (spikes per unit area).

            entityConfig.layerKernelSpikeFrequencyLearningRate += prefix + layerKernelSpikeFrequencyLearningRate;
            entityConfig.layerKernelSpikeFrequencyTarget += prefix + layerKernelSpikeFrequencyTarget;

            // Spike Learning and training parameters, time constants and spike density
            float layerConvSpikeDensityTarget = 0.1f; // What density of spikes do you want per sample? Note, this is BEFORE inhibition, which will sparsen the result.
            int dataRepeatPeriod = 3; // over how many data samples do you want to measure the average spike density, whichs gives the P for the PI controller?
            int layerConvSpikeIntegrationPeriod = 10; // how many periods are integrated to compute the integral term for the PI controller
            int layerConvSpikeUpdatePeriod = dataRepeatPeriod * imageRepeats;

            entityConfig.layerConvSpikeDensityTarget += prefix + layerConvSpikeDensityTarget;
            entityConfig.layerConvSpikeIntegrationPeriod += prefix + layerConvSpikeIntegrationPeriod;
            entityConfig.layerConvSpikeUpdatePeriod += prefix + layerConvSpikeUpdatePeriod;

            // Geometric parameters:
            entityConfig.layerInputPadding += prefix + layerInputPadding;
            entityConfig.layerInputStride  += prefix + layerInputStride;
            entityConfig.layerWidth  += prefix + lw;
            entityConfig.layerHeight += prefix + lh;
            entityConfig.layerDepth  += prefix + ld;
            entityConfig.layerfieldWidth += prefix + fw;
            entityConfig.layerfieldHeight += prefix + fh;
            entityConfig.layerfieldDepth += prefix + fd;
            entityConfig.layerPoolingWidth += prefix + pw;
            entityConfig.layerPoolingHeight += prefix + ph;

            // Auto calculate layer widths and heights
            iw = lw / pw;
            ih = lh / ph;
            id = ld;
        }

        Framework.SetConfig( entityName, entityConfig );
    }

    // Input 1: 28 x 28 (x2)
    // Window: 5x5, stride 2, padding = 0
    //     00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 |
    //  F1 -- -- -- -- --                                                                      |
    //  F2          -- -- -- -- --                                                             |
    //  F3                   -- -- -- -- --                                                    |
    //  F4                            -- -- -- -- --                                           |
    //  F5                                     -- -- -- -- --                                  |
    //  F6                                              -- -- -- -- --                         |
    //  F7                                                       -- -- -- -- --
    //  F8                                                                -- -- -- -- --
    //  F9                                                                         -- -- -- -- xx

    // Max Pooling:
    // 0 1  2 3  4 5  6 7  8 *
    //  0    1    2    3    4
    // So output is 5x5

    // Input 1: 5 x 5 (x30)
    // Window: 5x5, stride 1, padding = 0
    //     00 01 02 03 04
    //  F1 -- -- -- -- --
    // Output is 1x1 by depth 100



    // Input 1: 28 x 28 (x2 in Z, for DoG + and -)
    // Window: 5x5, stride 1, padding = 0
    // iw - kw +1 = 28-5+1 = 24
    //     00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 |
    //  F1 -- -- -- -- --                                                                      |
    //  F2    -- -- -- -- --                                                             |
    //  F3       -- -- -- -- --                                                    |
    //  F4          -- -- -- -- --                                           |
    //  F5             -- -- -- -- --                                  |
    //  F6                -- -- -- -- --                         |
    //  F7                   -- -- -- -- --
    //  F8                      -- -- -- -- --
    //  F9                         -- -- -- -- xx
    //  F10                           -- -- -- -- xx
    //  F11                              -- -- -- -- xx
    //  F12                                 -- -- -- -- xx
    //  F13                                    -- -- -- -- xx
    //  F14                                       -- -- -- -- xx
    //  F15                                          -- -- -- -- xx
    //  F16                                             -- -- -- -- xx
    //  F17                                                -- -- -- -- xx
    //  F18                                                   -- -- -- -- xx
    //  F19                                                      -- -- -- -- xx
    //  F20                                                         -- -- -- -- xx
    //  F21                                                            -- -- -- -- xx
    //  F22                                                               -- -- -- -- xx
    //  F23                                                                  -- -- -- -- xx
    //  F24                                                                     -- -- -- -- xx
    //     00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 |

    // 24 cells wide in conv 0
    // Max pooling size=2 stride=2
    //     00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 |
    // F1  -- --
    // F2        -- --
    // F3              -- --
    // F4                    -- --
    // F5                          -- --
    // F6                                -- --
    // F7                                      -- --
    // F8                                            -- --
    // F9                                                  -- --
    // F10                                                       -- --
    // F11                                                             -- --
    // F12                                                                   -- --
    //     00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 |

    // Conv layer 2: 12 inputs. Needs 8 cells.
    // iw - kw +1 = 12-5+1 = 8
    //     00 01 02 03 04 05 06 07 08 09 10 11 |
    //  F1 -- -- -- -- --
    //  F2    -- -- -- -- --
    //  F3       -- -- -- -- --
    //  F4          -- -- -- -- --
    //  F5             -- -- -- -- --
    //  F6                -- -- -- -- --
    //  F7                   -- -- -- -- --
    //  F8                      -- -- -- -- --
    //     00 01 02 03 04 05 06 07 08 09 10 11 |

    // Max pooling layer 2: over all, so output 1x1 by depth.
}
