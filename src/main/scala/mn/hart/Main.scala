package mn.hart

import java.time.Duration

import com.microsoft.azure.servicebus.{MessageHandlerOptions, QueueClient, ReceiveMode}
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

    // Create a static stream with a few hundred RDDs, each containing a set of test strings.
    val stream = ExampleStream(ssc)

    val transformedStream = stream.transform(rdd => {
      // The body of transform will execute on the driver for each RDD.
      // It's here that we can fetch the latest config from the Service Bus thread.
      val config = ConfigManagerSingleton.get()

      rdd.map(value => config.prefix + value)
    })

    // Print each RDD to the console so we can see the output over time.
    transformedStream.print()

    ssc.checkpoint(checkpointDir)
    ssc
  }

  // Initialize ConfigManagerSingleton, which is used to pass updated configs from Service Bus to Spark
  ConfigManagerSingleton.init(
    initialConfig = Config("Hello ")
  )

  // Initialize connection to Service Bus
  val queueClient = new QueueClient(
    new ConnectionStringBuilder(busConnStr, busQueueName),
    ReceiveMode.PEEKLOCK
  )

  try {
    // Register config manager as handler
    queueClient.registerMessageHandler(ConfigManagerSingleton,
      new MessageHandlerOptions(
        1, // Max concurrent calls
        true,
        Duration.ofMinutes(5)
      )
    )

    // Get StreamingContext from checkpoint data or create a new one
    val context = StreamingContext.getOrCreate(checkpointDir, createStreamingContext)

    // Start the context
    context.start()
    context.awaitTermination()
  } finally {
    queueClient.close()
  }
}