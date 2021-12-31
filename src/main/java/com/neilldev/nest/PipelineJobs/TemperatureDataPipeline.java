package com.neilldev.nest.PipelineJobs;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.neilldev.nest.Data.TempDataObj;
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

public class TemperatureDataPipeline {
    public static void main(String[] args) {

        String SUBSCRIPTION = "projects/thermostat-data--1640722466265/subscriptions/nest-device-events-a";
        String DESTINATION_TABLE_SPEC = "thermostat-data--1640722466265:thermo_events.temp_events";
        String PROJECT_ID = "thermostat-data--1640722466265";

        TableSchema TABLE_SCHEMA =

                new TableSchema()
                        .setFields(
                                Arrays.asList(
                                        new TableFieldSchema()
                                                .setName("temp_f")
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
        options.setStagingLocation("gs://thermoevents_tmp/pipeline_tmp_temp");
        options.setEnableStreamingEngine(true);
        options.setWorkerMachineType("e2-small");
        //ArrayList<String> experiments = new ArrayList<String>();
        //experiments.add("use_runner_v2");
        //options.setExperiments(experiments);

        Pipeline pipe = Pipeline.create(options);

        PCollection<TempDataObj> messages = pipe.apply("ReadPubSub", PubsubIO.readStrings().fromSubscription(SUBSCRIPTION)).apply("Filter non TEMP events", Filter.by(msg -> StringUtils.containsIgnoreCase(msg, "ambientTemperatureCelsius"))).apply("Format for BQ", ParDo.of(new parsePubSubToTempDataObjFn()));
        messages.apply("WriteToBQ", BigQueryIO.<TempDataObj>write()
                .to(DESTINATION_TABLE_SPEC)
                .withSchema(TABLE_SCHEMA)
                .withFormatFunction((TempDataObj elem) -> new TableRow().set("temp_f", elem.temp).set("timestamp_utc", elem.timestamp))
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
        );


        pipe.run().waitUntilFinish();

    }

}

class parsePubSubToTempDataObjFn extends DoFn<String, TempDataObj> implements Serializable {
    @ProcessElement
    public void processElement(@Element String message, OutputReceiver<TempDataObj> out) {
        JSONObject json = new JSONObject(message);
        String temp_f = null;
        try {
            float temp_c = json.getJSONObject("resourceUpdate").getJSONObject("traits").getJSONObject("sdm.devices.traits.Temperature").getFloat("ambientTemperatureCelsius");
            temp_f = String.valueOf((temp_c * 1.8) + 32);
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

        out.output(new TempDataObj(temp_f, ts_final));
    }

}

