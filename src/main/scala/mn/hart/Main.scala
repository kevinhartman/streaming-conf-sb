package mn.hart

import com.microsoft.azure.servicebus.{QueueClient, ReceiveMode}
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Main extends App {

  if (args.length != 3) throw new IllegalArgumentException("req args: checkpointDir, serviceBusConnStr, serviceBusQueue")

  // Get checkpoint dir from args
  val checkpointDir :: busConnStr :: busQueueName :: _ = args.toList

  private def createStreamingContext(): StreamingContext = {
    val conf = new SparkConf()
      .setAppName("streaming-conf-sb")
      .setIfMissing("spark.master", "local[*]")

    val sparkContext = new SparkContext(conf)
    val ssc = new StreamingContext(sparkContext, Seconds(5))
    ssc.checkpoint(checkpointDir)
    ssc
  }

  // Initialize connection to Service Bus
  val queueClient = new QueueClient(
    new ConnectionStringBuilder(busConnStr, busQueueName),
    ReceiveMode.PEEKLOCK
  )

  queueClient.registerMessageHandler(ConfigurationProviderSingleton.ConfigurationMessageHandler)

  try {
    // Get StreamingContext from checkpoint data or create a new one
    val context = StreamingContext.getOrCreate(checkpointDir, createStreamingContext)

    // Start the context
    context.start()
    context.awaitTermination()
  } finally {
    queueClient.close()
  }
}