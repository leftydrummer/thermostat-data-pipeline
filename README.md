This is a small project I made to try out some cloud/big data concepts. (Though the amount of data here isn't so big)

I wanted to make use of data coming from a real source, in real time, in the real world, instead of faking it or using an 
existing data set. After some thinking I decided to see if I could get sensor data from my Nest Smart Thermostat. Turns out our friends at the Google make it relatively simple to get sensor data from these devices as events. 

The final product is a cloud-based streaming data pipeline that processes sensor data from my thermostat, and publishes the results to a cloud warehouse.

The pipeline breaks down into the following parts:

1. Google's Device Access service posts device events from my thermostat to a Topic in Google Cloud's Pub/Sub service
2. Multiple pull Subscriptions are set up for this Topic.
3. For each Subscription, there are Google Dataflow streaming jobs that process the incoming messages - extracting and formatting the needed information. It then writes the results to Google BigQuery. The jobs are built with Apache Beam and written in Java.

Bonus- I did some basic visualizations using Google Data Studio. 

# Project Breakdown

This entire project lives in a Google Cloud Platform project called ``thermostat-data-pipeline``

## Getting thermostat events

The setup for getting device events from the thermostat was relatively simple, thanks to Google's (new?) Device Access service. For a fee you can register your devices for API/programmatic access. I then created a project in Device Access. Then I registered my GCP project as an Oauth client- allowing us to use the Device Access APIs in our GCP project. Device Access gives us a Pub/Sub Topic out of the box to use to get device events. 

![](/images/deviceaccess.png)

## Pub/Sub

Currently, I have two subscriptions on the device events topic- `nest-device-events-a` and `nest-device-events-b`. This allows me to have separate downstream processes that receive all device events. My Dataflow jobs filter ignore event data that is unneeded. 

The screenshot below shows an example of the messages coming from the thermostat

![](/images/pubsub2.png)

## Dataflow/Apache Beam

For the purposes of this pipeline, I only need a small amount of data. I want to know when the ambient temperature changes, and when the ambient humidity changes. 

I currently have 2 Dataflow jobs -- `HumidityDataPipeline` and `TemperatureDataPipeline`

These jobs use the Apache Beam programming model and are written in Java. The basic flow for both jobs is:

1. Set the pipeline options for running on the Dataflow service
2. Outline the destination BigQuery table schema
3. Read in PubSub events
4. Filter out non-pertinent messages
5. Extract the timestamp and new sensor value for either `TEMP` or `HUMIDITY`
6. Write the result as a row to BigQuery

For the Temperature job we also calculate the Fahrenheit value, as the thermostat sends it in Celsius. The code for these jobs is available in the above repo under [PipelineJobs](src/main/java/com/neilldev/nest/PipelineJobs).

![](/images/dataflow.png)

## BigQuery

My BigQuery tables have a very simple schema. We have a dataset called `thermo_events` and two tables:

`humidity_events` 
 - `timestamp_utc`: type `TIMESTAMP`, `humidity_percent`: type `FLOAT` 

`temp_events`
- `timestamp_utc`: type `TIMESTAMP`, `temp_f`: type `FLOAT` 

![](/images/bigquery.png)

## Google Data Studio visualizations

Finally, in order to do something neat with all this data, I made some basic charts in Google Data Studio to visualize the BigQuery data. Data Studio comes with an out-of-the-box connector for BigQuery, making it very easy to import and use data from those tables.

This report shows sensor events over the last day, as well as the average reading over the last 24 hours. 

You can view the live report [here](https://datastudio.google.com/reporting/2e08ab9b-9beb-4a15-8214-a287ffff95c6)! 

![](/images/gds.png)






