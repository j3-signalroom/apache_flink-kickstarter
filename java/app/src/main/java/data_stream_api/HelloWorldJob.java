package data_stream_api;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public class HelloWorldJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	    DataStream<String> msg = env.fromData("Hello", "World");
        msg.print();
	    env.execute("HelloWorld");
    }
}
