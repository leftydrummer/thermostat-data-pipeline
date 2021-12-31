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


