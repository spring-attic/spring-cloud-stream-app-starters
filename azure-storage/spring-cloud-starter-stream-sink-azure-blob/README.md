# spring-cloud-stream-wasb-sink
#### A Windows Azure Storage Blob sink module for Spring Cloud Stream

This module is an MVP implementation of a BlockBlob-based sink module
for text payloads originating from a Spring Cloud Dataflow stream.

An example stream definition:

```
dataflow:> app register --type sink --name wasb --uri file:///Users/kdunn/.m2/repository/io/pivotal/pde/wasb-sink/0.0.1-SNAPSHOT/wasb-sink-0.0.1-SNAPSHOT.jar

# CloudBlockBlob (every payload overwrites)
dataflow:> stream create --name testWasb --definition 'time | wasb --accountName="scdftest" --accountKey="<YOUR STORAGE ACCOUNT KEY HERE>" --containerName="time" --blobName="test" ' --deploy

# CloudAppendBlob (every payload appends)
dataflow:> stream create --name testWasb --definition 'time | wasb --accountName="scdftest" --accountKey="<YOUR STORAGE ACCOUNT KEY HERE>" --containerName="time" --blobName="testAppend" --appendOnly=true --overwriteExistingAppend=true ' --deploy
```
