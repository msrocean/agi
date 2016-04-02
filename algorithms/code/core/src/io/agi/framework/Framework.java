package io.agi.framework;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.agi.core.orm.NamedObject;
import io.agi.core.util.FileUtil;
import io.agi.framework.persistence.Persistence;
import io.agi.framework.persistence.models.ModelData;
import io.agi.framework.persistence.models.ModelDataReference;
import io.agi.framework.persistence.models.ModelEntity;
import io.agi.framework.persistence.models.ModelEntityPathConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Functions used throughout the experimental framework, i.e. not specific to Entity or Node.
 * Created by dave on 2/04/16.
 */
public class Framework {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Modifies the database to make the reference _entityName-suffix Data a reference input to the input _entityName-suffix.
     *
     * @param p
     * @param inputEntity
     * @param inputSuffix
     * @param referenceEntity
     * @param referenceSuffix
     */
    public static void SetDataReference(
            Persistence p,
            String inputEntity,
            String inputSuffix,
            String referenceEntity,
            String referenceSuffix ) {
        String inputKey = NamedObject.GetKey(inputEntity, inputSuffix);
        String refKey = NamedObject.GetKey( referenceEntity, referenceSuffix );
        SetDataReference(p, inputKey, refKey);
    }

    /**
     * Modifies the database to make the reference _entityName-suffix Data a reference input to the input _entityName-suffix.
     *
     * @param p
     * @param dataKey
     * @param refKeys
     */
    public static void SetDataReference(
            Persistence p,
            String dataKey,
            String refKeys ) {
        ModelData modelData = p.getData(dataKey);

        if ( modelData == null ) {
            modelData = new ModelData( dataKey, refKeys );
        }

        modelData._refKeys = refKeys;
        p.setData(modelData);
    }

    /**
     * Allows a single config property to be obtained.
     * @param entityName
     * @param configPath
     */
    public static String GetConfig( String entityName, String configPath, Persistence p ) {
        ModelEntity me = p.getEntity(entityName);
        JsonParser parser = new JsonParser();
        JsonObject jo = parser.parse( me.config ).getAsJsonObject();

        // navigate to the nested property
        JsonElement je = GetNestedProperty( jo, configPath );

        return je.getAsString();
    }

    /**
     * Allows a single config property to be modified.
     * @param entityName
     * @param configPath
     */
    public static void SetConfig( String entityName, String configPath, String value, Persistence p ) {
        ModelEntity me = p.getEntity( entityName );
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse( me.config ).getAsJsonObject();

        // navigate to the nested property
        JsonObject parent = root;
        String[] pathParts = configPath.split(".");
        String part = null;
        int index = 0;
        int maxIndex = pathParts.length -2; // NOTE: one before the one we're looking for

        while( index < maxIndex ) {
            part = pathParts[ index ];
            JsonElement child = parent.get( part );

            ++index;

            parent = (JsonObject)child;
        }

        // replace the property:
        parent.remove(part);
        parent.addProperty(part, value);

        // re-serialize the whole thing
        me.config = root.getAsString();
        p.setEntity( me );
    }

    public static JsonElement GetNestedProperty( JsonObject root, String path ) {
        // navigate to the nested property
        JsonElement je = root;
        String[] pathParts = path.split(".");
        String part = null;
        int index = 0;
        int maxIndex = pathParts.length -1;

        while( index < maxIndex ) {
            part = pathParts[ index ];

            JsonObject joParent = (JsonObject)je; // there is more to find
            JsonElement jeChild = joParent.get( part );

            ++index;

            je = jeChild;
        }

        return je;
    }

    public static void LoadEntities( String file, Persistence p ) {
        Gson gson = new Gson();

        try {
            String jsonEntity = FileUtil.readFile(file);

            Type listType = new TypeToken< List< ModelEntity > >() {
            }.getType();
            List< ModelEntity > entities = gson.fromJson( jsonEntity, listType );

            for( ModelEntity modelEntity : entities ) {
                logger.info( "Persisting Entity of type: " + modelEntity.type + ", that is hosted at Node: " + modelEntity.node );
                p.setEntity( modelEntity );
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
            System.exit( -1 );
        }
    }

    public static void LoadDataReferences( String file, Persistence p ) {
        Gson gson = new Gson();
        try {
            String jsonEntity = FileUtil.readFile( file );

            Type listType = new TypeToken< List<ModelDataReference> >() {
            }.getType();

            List< ModelDataReference > references = gson.fromJson( jsonEntity, listType );
            for( ModelDataReference modelDataReference : references ) {
                logger.info( "Persisting data input reference for data: " + modelDataReference.dataKey + " with input data keys: " + modelDataReference.refKeys );
                Framework.SetDataReference( p, modelDataReference.dataKey, modelDataReference.refKeys );
            }
        }
        catch ( Exception e ) {
            logger.error(e.getStackTrace());
            System.exit( -1 );
        }
    }

    public static void LoadConfigs( String file, Persistence p ) {
        Gson gson = new Gson();
        try {
            String jsonEntity = FileUtil.readFile( file );

            Type listType = new TypeToken< List<ModelEntityPathConfig> >() {
            }.getType();
            List<ModelEntityPathConfig> modelConfigs = gson.fromJson( jsonEntity, listType );

            for ( ModelEntityPathConfig modelConfig : modelConfigs ) {

                logger.info( "Persisting entity: " + modelConfig._entityName + " config path: " + modelConfig._configPath  + " value: " + modelConfig._configValue );

                Framework.SetConfig( modelConfig._entityName, modelConfig._configPath, modelConfig._configValue, p );
//                for ( String keySuffix : modelConfig._configPathValues.keySet() ) {
//                    String value = modelConfig._configPathValues.get( keySuffix );
//
//                    //System.out.println( "\tKeySuffix: " + keySuffix + ", Value: " + value );
//
//                    //String key = Keys.concatenate( modelPropertySet._entityName, keySuffix );
//                    _p.setPropertyString( key, value );
//                }

            }
        }
        catch ( Exception e ) {
            logger.error(e.getStackTrace());
            System.exit( -1 );
        }
    }

}
