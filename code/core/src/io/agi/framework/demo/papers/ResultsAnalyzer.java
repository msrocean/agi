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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agi.core.data.Data;
import io.agi.core.data.FloatArray;
import io.agi.core.math.Statistics;
import io.agi.core.util.FileUtil;
import io.agi.core.util.PropertiesUtil;
import io.agi.framework.Framework;
import io.agi.framework.Main;
import io.agi.framework.factories.CommonEntityFactory;
import io.agi.framework.persistence.models.ModelData;
import io.agi.framework.persistence.models.ModelEntity;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by dave on 5/02/17.
 */
public class ResultsAnalyzer {

    /**
     * Usage: Expects some arguments. These are:
     * 0: data file (json)
     * 1: truth data name (actual, correct labels)
     * 2: predicted labels data name (i.e. predicted label output)
     *
     * It is expected both data specified are vectors of the same size.
     *
     * @param args
     */
    public static void main( String[] args ) {

        // Optionally set a global prefix for entities
        if( args.length != 5 ) {
            System.err.println( "Bad arguments. Should be: data_file data_name_truth data_name_predicted OFFSET LENGTH " );
            System.exit( -1 );
        }

        String dataFile = args[ 0 ];
        String dataNameTruth = args[ 1 ];
        String dataNamePredicted = args[ 2 ];
        int offset = Integer.parseInt( args[ 3 ] );
        int length = Integer.parseInt( args[ 4 ] );

        System.out.println( "Data file: " + dataFile );
        System.out.println( "Data name truth: " + dataNameTruth );
        System.out.println( "Data name predicted: " + dataNamePredicted );
        System.out.println( "Data offset: " + offset );
        System.out.println( "Data length: " + length );

        try {
            Gson gson = new Gson();

            String jsonData = FileUtil.readFile( dataFile );

            Type listType = new TypeToken< List< ModelData > >() {}.getType();

            List< ModelData > modelDatas = gson.fromJson( jsonData, listType );

            Data truth = null;
            Data predicted = null;

            for( ModelData modelData : modelDatas ) {
                if( modelData.name.equals( dataNameTruth ) ) {
                    System.out.println( "Found data: " + modelData.name );

                    truth = modelData.getData();
                }
                else if( modelData.name.equals( dataNamePredicted ) ) {
                    System.out.println( "Found data: " + modelData.name );

                    predicted = modelData.getData();
                }
                else {
                    System.out.println( "Skipping data: " + modelData.name );
                }
            }

            if( truth == null ) {
                System.err.println( "Couldn't find truth labels data." );
            }

            if( predicted == null ) {
                System.err.println( "Couldn't find predicted labels data." );
            }

            if( ( truth == null ) || ( predicted == null ) ) {
                System.exit( -1 );
            }

            analyze( truth, predicted, offset, length );
        }
        catch( Exception e ) {
            System.err.println( e.getStackTrace() );
            System.exit( -1 );
        }

    }

    public static void analyze( Data truth, Data predicted, int offset, int length ) {
        //
        // F-score (precision+recall), confusion matrix
        // find unique labels in truth
        HashSet< Float > labels = Statistics.unique( truth );

        ArrayList< Float > sorted = new ArrayList< Float >();
        sorted.addAll( labels ); // all unique labels in order
        Collections.sort( sorted );

        // build an empty confusion matrix
        HashMap< Float, HashMap< Float, Integer > > errorTypeCount = new HashMap< Float, HashMap< Float, Integer > >();

        HashMap< Float, Integer > labelCount = new HashMap< Float, Integer >();
        HashMap< Float, Integer > labelCountPredictions = new HashMap< Float, Integer >();
        HashMap< Float, Integer > labelErrorFP = new HashMap< Float, Integer >();
        HashMap< Float, Integer > labelErrorFN = new HashMap< Float, Integer >();

        for( Float f : labels ) {

            labelCount.put( f, 0 );
            labelCountPredictions.put( f, 0 );
            labelErrorFP.put( f, 0 );
            labelErrorFN.put( f, 0 );

            HashMap< Float, Integer > hm = new HashMap< Float, Integer >();

            for( Float f2 : labels ) {
                hm.put( f2, 0 );
            }

            errorTypeCount.put( f, hm );
        }

        // calculate errors
        FloatArray errors = new FloatArray( truth.getSize() );

        int i0 = offset;
        int i1 = offset + length;

        if( offset < 0 ) {
            System.err.println( "Bad offset: Negative." );
            System.exit( -1 );
        }
        if( i1 > truth._values.length ) {
            System.err.println( "Bad offset/length combination: Out of range (size="+truth._values.length + ")." );
            System.exit( -1 );
        }

        // per class:
        // prec = tp / all pos in test.
        // recall = tp / truth pos.

        for( int i = i0; i < i1; ++i ) {
            float t = truth._values[ i ];
            float p = predicted._values[ i ];

            float error = 0f;
            if( t != p ) {
                error = 1f;

                HashMap< Float, Integer > hm = errorTypeCount.get( t );

                Integer n1 = hm.get( p );
                int n2 = n1 +1; // increment frequency

                hm.put( p, n2 );

                Integer n1p = labelErrorFP.get( p );
                int n2p = n1p +1; // increment frequency
                labelErrorFP.put( p, n2p );

                Integer n1n = labelErrorFN.get( t );
                int n2n = n1n +1; // increment frequency
                labelErrorFN.put( t, n2n );
            }
            else {
                Integer n1 = labelCount.get( t );
                int n2 = n1 +1; // increment frequency
                labelCount.put( t, n2 );
            }


            errors._values[ i ] = error;
        }

        // display stats.
        float sum = errors.sum();
        float count = length;//errors.getSize();
        float pc = 1f - ( sum / count );
        pc *= 100f;
        System.out.println();
        System.out.println( "Errors: " + (int)sum + " of " + (int)count + " = " + pc + "% correct." );
        System.out.println();
        System.out.println( "Confusion:" );
        System.out.println();

        System.out.println( "           <--- PREDICTED ---> " );
        System.out.print( "      " );
        for( Float fP : sorted ) {
            System.out.print( String.format( "%.1f", fP ) + " , " );
        }
        System.out.println();

        int w = 6; // todo make it number of digits in max( error-type-count ) + , + whatever padding needed
        int paddingChars = w - 2;

        String padding = "";
        for( int i = 0; i < paddingChars; ++i ) {
            padding = padding + " ";
        }

        for( Float fT : sorted ) {
            HashMap< Float, Integer > hm = errorTypeCount.get( fT );

            System.out.print( " " + String.format( "%.1f", fT ) + ", "  );

            for( Float fP : sorted ) {

                int frequency = hm.get( fP );

                System.out.print( frequency + "," + padding );
            }

            System.out.println();
        }

        System.out.println();
        System.out.println( "F-Score:" );
        System.out.println();
        // per class:
        // prec = tp / all pos in test.
        // recall = tp / truth pos.

        System.out.println( " Label, Err, TP, FP, TN, FN, T, F, F-Score"  );

        float b2 = 0;

        for( Float label : sorted ) {

            int fp = (int)labelErrorFP.get( label );
            int fn = (int)labelErrorFN.get( label );
            int t = (int)labelCount.get( label );
            int f = (int)count - t;

            int tp = t-fn;
            int tn = f-fp;
            int e = fp + fn;
            float denominator = (1f + b2) * tp + b2 * fn + fp;
            float score = (1f + b2)* tp / denominator;

            System.out.print( String.format( "%.1f", label ) + ", " + e  + ", " + tp + ", " + fp + ", " + tn + ", " + fn + ", " + t + ", " + f + ", " + score );

            System.out.println();

        }

    }
}