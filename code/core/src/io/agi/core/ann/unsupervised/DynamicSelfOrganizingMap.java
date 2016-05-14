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

package io.agi.core.ann.unsupervised;

import io.agi.core.data.Data;
import io.agi.core.data.FloatArray;
import io.agi.core.orm.ObjectMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Online algorithm with parameters to determine how input feature density affects cell density (elasticity).
 * See "Dynamic Self-Organising Map" by Nicolas Rougier and Yann Boniface (2010)
 * <p/>
 * Created by dave on 29/12/15.
 */
public class DynamicSelfOrganizingMap extends CompetitiveLearning {

    public DynamicSelfOrganizingMapConfig _c;
    public Data _inputValues;
    public Data _cellWeights;
    public Data _cellErrors;
    public Data _cellActivity;
    public Data _cellMask;

    public DynamicSelfOrganizingMap( String name, ObjectMap om ) {
        super( name, om );
    }

    public void setup( DynamicSelfOrganizingMapConfig c ) {
        _c = c;

        int inputs = c.getNbrInputs();
        int w = c.getWidthCells();
        int h = c.getHeightCells();

        _inputValues = new Data( inputs );
        _cellWeights = new Data( w, h, inputs );
        _cellErrors = new Data( w, h );
        _cellActivity = new Data( w, h );
        _cellMask = new Data( w, h );
    }

    public void reset() {
        _cellMask.set( 1.f );
        _cellWeights.setRandom( _c._r );
    }

    public Data getInput() {
        return _inputValues;
    }

    public void call() {
        update();
    }

    public void update() {
        CompetitiveLearning.sumSqError( _c, _inputValues, _cellWeights, _cellErrors );

        // get the top N cells
        int maxRank = 1;
        TreeMap< Float, ArrayList< Integer > > ranking = new TreeMap< Float, ArrayList< Integer > >();
        CompetitiveLearning.findBestNCells( _c, _cellMask, _cellErrors, _cellActivity, maxRank, false, ranking );

        if( ranking.isEmpty() ) {
            return;
        }

        Float bestError = ranking.keySet().iterator().next();
        int bestCell = ranking.get( bestError ).get( 0 );

        trainWithInput( _c, _inputValues, _cellWeights, _cellErrors, bestCell );
    }

    public static void trainWithInput(
            DynamicSelfOrganizingMapConfig c,
            FloatArray inputValues,
            FloatArray cellWeights,  // Size = cells * inputs
            FloatArray cellSumSqError ) {
        int winningCell = cellSumSqError.minValueIndex();
        trainWithInput( c, inputValues, cellWeights, cellSumSqError, winningCell );
    }

    public static void trainWithInput(
            DynamicSelfOrganizingMapConfig c,
            FloatArray inputValues,
            FloatArray cellWeights,  // Size = cells * inputs
            FloatArray cellSumSqError,
            int winningCell ) {

        int inputs = c.getNbrInputs();
        int cells = c.getNbrCells();
        int w = c.getWidthCells();
        int h = c.getWidthCells();
        float elasticity = c.getElasticity();
        float learningRate = c.getLearningRate();

        float inputMin = 0.f;
        float inputMax = 1.f;

        int yw = winningCell / w;
        int xw = winningCell % w;

        float xRelWinner = xw / ( float ) w;
        float yRelWinner = yw / ( float ) h;

        float maxSumError = ( float ) Math.sqrt( inputs );
        float winningErrorSq = cellSumSqError._values[ winningCell ];
        float winningNormEucNorm = ( float ) Math.sqrt( winningErrorSq ) / maxSumError;
        float winningNormEucNormSq = winningNormEucNorm * winningNormEucNorm;

        // update weights to be closer to observations
        int cell = 0;
        for( int y = 0; y < h; ++y ) { // for each som cell
            for( int x = 0; x < w; ++x ) { // for each som cell

                float xRel = x / ( float ) w;
                float yRel = y / ( float ) h;

                float dx = xRel - xRelWinner;
                float dy = yRel - yRelWinner;

                // http://en.wikipedia.org/wiki/Norm_%28mathematics%29
                float cellDistanceSq = dx * dx + dy * dy;

                float sumSqError = cellSumSqError._values[ cell ];
                float sumError = ( float ) Math.sqrt( sumSqError );
                float normEucNorm = sumError / maxSumError;
                float learningRateWeight = normEucNorm * learningRate;
                float cellDistanceWeight = getCellDistanceWeight( elasticity, cellDistanceSq, winningNormEucNormSq );

                for( int i = 0; i < inputs; ++i ) { // for each input

                    int inputOffset = cell * inputs + i;

                    float inputValue = inputValues._values[ i ]; // error from ci to cell
                    float weightOld = cellWeights._values[ inputOffset ]; // error from ci to cell
                    float weightNew = getUpdatedWeight( inputValue, weightOld, inputMin, inputMax, learningRateWeight, cellDistanceWeight );//, c._noiseMagnitude );

                    cellWeights._values[ inputOffset ] = weightNew; // error from ci to cell
                }

                ++cell;
            }
        }
    }

    public static void trainWithSparseInput(
            DynamicSelfOrganizingMapConfig c,
            FloatArray inputValues,
            FloatArray cellWeights,  // Size = cells * inputs
            FloatArray cellSumSqError,
            int winningCell ) {

        HashSet< Integer > activeInputValues = new HashSet< Integer >();
        int inputs = c.getNbrInputs();
        for( int i = 0; i < inputs; ++i ) { // for each input
            float inputValue = inputValues._values[ i ]; // error from ci to cell

            if( inputValue > 0.f ) {
                activeInputValues.add( i );
            }
        }

        trainWithSparseInput( c, activeInputValues, cellWeights, cellSumSqError, winningCell );
    }

    public static void trainWithSparseInput(
            DynamicSelfOrganizingMapConfig c,
            HashSet< Integer > activeInputValues,
            FloatArray cellWeights,  // Size = cells * inputs
            FloatArray cellSumSqError,
            int winningCell ) {
        int inputs = c.getNbrInputs();
        int cells = c.getNbrCells();
        int w = c.getWidthCells();
        int h = c.getWidthCells();
        float elasticity = c.getElasticity();
        float learningRate = c.getLearningRate();

        float inputMin = 0.f;
        float inputMax = 1.f;

        int yw = winningCell / w;
        int xw = winningCell % w;

        float xRelWinner = xw / ( float ) w;
        float yRelWinner = yw / ( float ) h;

        float maxSumError = ( float ) Math.sqrt( inputs );
        float winningErrorSq = cellSumSqError._values[ winningCell ];
        float winningNormEucNorm = ( float ) Math.sqrt( winningErrorSq ) / maxSumError;
        float winningNormEucNormSq = winningNormEucNorm * winningNormEucNorm;

        // update weights to be closer to observations
        int cell = 0;
        for( int y = 0; y < h; ++y ) { // for each som cell
            for( int x = 0; x < w; ++x ) { // for each som cell
                float xRel = x / ( float ) w;
                float yRel = y / ( float ) h;

                float dx = xRel - xRelWinner;
                float dy = yRel - yRelWinner;

                // http://en.wikipedia.org/wiki/Norm_%28mathematics%29
                float cellDistanceSq = dx * dx + dy * dy;

                float errorSq = cellSumSqError._values[ cell ];
                float error = ( float ) Math.sqrt( errorSq );
                float learningRateWeight = error * learningRate;
                float cellDistanceWeight = getCellDistanceWeight( elasticity, cellDistanceSq, winningErrorSq );

                int inputOffset = cell * inputs;

                for( int i = 0; i < inputs; ++i ) { // for each input

                    //int inputOffset = cell * c._i + i;

                    float inputValue = 0.f; //inputValues._values[ i ]; // error from ci to cell
                    if( activeInputValues.contains( i ) ) {
                        inputValue = 1.f;
                    }

                    float weightOld = cellWeights._values[ inputOffset ]; // error from ci to cell
                    float weightNew = getUpdatedWeight( inputValue, weightOld, inputMin, inputMax, learningRateWeight, cellDistanceWeight );

                    cellWeights._values[ inputOffset ] = weightNew; // error from ci to cell

                    ++inputOffset;
                }

                ++cell;
            }
        }
    }

    public static float getUpdatedWeight(
            float value,
            float weightOld,
            float weightMin,
            float weightMax,
            float learningRateWeight,
            float cellDistanceWeight ) {

        float diff = value - weightOld; // gives sign and weight
        float delta = learningRateWeight * cellDistanceWeight * diff; // if error = 0 no change.

        float weightNew = weightOld;
        weightNew += delta;
//        weightNew += ( ( RandomInstance.random() - 0.5 ) * noiseMagnitude );
        weightNew = Math.min( weightMax, weightNew );
        weightNew = Math.max( weightMin, weightNew );
        //Unit..check( weightNew );
        return weightNew;
    }

    public static float getCellDistanceWeight( float elasticity, float cellDistanceSq, float valueDistanceSq ) {
        if( valueDistanceSq <= 0.f ) {
            return 0.f;
        }
        float b = cellDistanceSq / valueDistanceSq;
        float a = 1.f / ( elasticity * elasticity );
        float product = a * b;
        float result = ( float ) Math.exp( -product );
        //Maths.check( result );
        return result;
    }

}
