package com.neilldev.nest.PipelineJobs;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.neilldev.nest.Data.HumidDataObj;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class HumidityDataPipeline {
    public static void main(String[] args) {

        String SUBSCRIPTION = "projects/thermostat-data--1640722466265/subscriptions/nest-device-events-b";
        String DESTINATION_TABLE_SPEC = "thermostat-data--1640722466265:thermo_events.humidity_events";
        String PROJECT_ID = "thermostat-data--1640722466265";

        TableSchema TABLE_SCHEMA =

                new TableSchema()
                        .setFields(
                                Arrays.asList(
                                        new TableFieldSchema()
                                                .setName("humidity_percent")
                                                .setType("FLOAT")
                                                .setMode("NULLABLE"),
                                        new TableFieldSchema()
                                                .setName("timestamp_utc")
                                                .setType("TIMESTAMP")
                                                .setMode("NULLABLE")));

        DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
        options.setRunner(DataflowRunner.class);
        options.setProject(PROJECT_ID);
        options.setRegion("us-central1");
        options.setNumberOfWorkerHarnessThreads(1);
        options.setMaxNumWorkers(1);
        options.setStagingLocation("gs://thermoevents_tmp/pipeline_stg_humid");
        options.setEnableStreamingEngine(true);
        options.setWorkerMachineType("e2-small");


        Pipeline pipe = Pipeline.create(options);

        PCollection<HumidDataObj> messages = pipe.apply("ReadPubSub", PubsubIO.readStrings().fromSubscription(SUBSCRIPTION)).apply("Filter non HUMIDITY events", Filter.by(msg -> StringUtils.containsIgnoreCase(msg, "ambientHumidityPercent"))).apply("Format for BQ", ParDo.of(new parsePubSubToHumidDataObjFn()));
        messages.apply("WriteToBQ", BigQueryIO.<HumidDataObj>write()
                .to(DESTINATION_TABLE_SPEC)
                .withSchema(TABLE_SCHEMA)
                .withFormatFunction((HumidDataObj elem) -> new TableRow().set("humidity_percent", elem.humidity_percent).set("timestamp_utc", elem.timestamp))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
        );

        pipe.run().waitUntilFinish();

    }

}

class parsePubSubToHumidDataObjFn extends DoFn<String, HumidDataObj> implements Serializable {
    @ProcessElement
    public void processElement(@Element String message, OutputReceiver<HumidDataObj> out) {

        JSONObject json = new JSONObject(message);

        String humidity = null;
        try {
             humidity = String.valueOf(json.getJSONObject("resourceUpdate").getJSONObject("traits").getJSONObject("sdm.devices.traits.Humidity").getFloat("ambientHumidityPercent"));

        } catch (JSONException e) {

        }
        String ts_final = null;

        try {
            String timestamp = json.getString("timestamp");
            String datePart = timestamp.split("T")[0];
            String timePart = timestamp.split("T")[1].substring(0, 8);
            ts_final = datePart + " " + timePart;
        } catch (JSONException e) {
        }

        out.output(new HumidDataObj(humidity, ts_final));
    }

}

