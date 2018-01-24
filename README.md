# streaming-conf-sb
Live configuration of Spark Streaming applications using Azure Service Bus sample project.

# Notes
* As a prerequisite to running this project, you'll need to have already created an Azure Service Bus queue within your Azure subscription.
* Recovery from checkpoint will not work as is since the example stream is based on the QueueStream, which cannot be checkpointed. You'll need to clear your checkpoint directory in-between runs of the application. This is intentianal, however, in order to illustrate how this approach *should* be integrated with Spark checkpointing (as a sort of template for "real" apps). Replacing the example stream with a checkpoint-able stream should work out of the box.
