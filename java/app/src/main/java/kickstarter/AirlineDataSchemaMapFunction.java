/**
 * Copyright (c) 2024 Jeffrey Jonathan Jennings
 * 
 * @author Jeffrey Jonathan Jennings (J3)
 * 
 * 
 * An Apache Flink custom data source stream is a user-defined source of data that
 * is integrated into a Flink application to read and process data from non-standard
 * or custom sources. This custom source can be anything that isn't supported by Flink
 * out of the box, such as proprietary REST APIs, specialized databases, custom hardware 
 * interfaces, etc.  This code uses a Custom Data Source Stream to compose the AirlineData 
 * Schema during the initial start of the Flink App, then caches the properties for use by
 * any subsequent events that need these properties.
 */
package kickstarter;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import java.util.concurrent.atomic.AtomicReference;

import kickstarter.model.*;


/**
 * This class creates a Custom Source Data Stream a Custom Data Source Stream to compose the
 * AirlineData Schema during the initial start of the Flink App, then caches the properties 
 * for use by any subsequent events that need these properties.
 */
public class AirlineDataSchemaMapFunction extends RichMapFunction<org.apache.avro.Schema, org.apache.avro.Schema>{
    private transient AtomicReference<org.apache.avro.Schema> _schema;


    /**
     * Default constructor.
     */
    public AirlineDataSchemaMapFunction() {}

    /**
     * This method is called once per parallel task instance when the job starts.
     * In which, it gets the AirlineData schema.  Then the schema are stored in the
     * class org.apache.avro.Schema.
     * 
     * @parameters The configuration containing the parameters attached to the
     * contract.
     */
    @Override
    public void open(Configuration configuration) {
        this._schema = new AtomicReference<>(AirlineData.buildSchema().rawSchema());
    }

    /**
     * This method is called for each element of the input stream.
     * 
     * @param value - The input value.
     * @return The result of the map operation.
     */
    @Override
    public org.apache.avro.Schema map(org.apache.avro.Schema value) {
        return(this._schema.get());
    }
    
    /**
     * This method is called when the task is canceled or the job is stopped.
     * For this particular class, it is not used.
     * 
     * @throws Exception - Implementations may forward exceptions, which are
     * caught.
     */
    @Override
    public void close() throws Exception {}
}
