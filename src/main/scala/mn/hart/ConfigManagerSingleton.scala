package mn.hart

import java.util.concurrent.{CompletableFuture, SynchronousQueue, TimeUnit}

import com.microsoft.azure.servicebus.{ExceptionPhase, IMessage, IMessageHandler}
import net.liftweb.json._

object ConfigManagerSingleton extends IMessageHandler {
  private val nextConfig: SynchronousQueue[Config] = new SynchronousQueue[Config]()

  @volatile private var config: Config = _

  /**
    * Initialize the singleton with an initial [[Config]] to be used until an update config arrives
    * on the Service Bus.
    * @param initialConfig The initial [[Config]] instance.
    */
  def init(initialConfig: Config): Unit = {
    config = initialConfig
  }

  /**
    * Gets the latest [[Config]].
    *
    * Note: This method is synchronized to guarantee that the latest config sent on Service Bus is used
    *       (various interleaving-s of multiple calls to this method could otherwise leave [[config]] with
    *       a stale value).
    * @return
    */
  def get(): Config = this.synchronized {
    // Grab the next config from the service bus client thread if one is ready.
    // If not, either no update requests have arrived on the service bus queue, or the service bus client thread is
    // still fulfilling a request (and hence the Delta is not yet available). Return the current config.
    val value = Option(nextConfig.poll(0, TimeUnit.SECONDS))

    if (value.isDefined) {
      config = value.get
    }

    config
  }

  /**
    * Called by Service Bus client library when an exception occurs during message processing.
    */
  override def notifyException(exception: Throwable, phase: ExceptionPhase): Unit = {
    sys.error(exception.toString)
  }

  /**
    * Called by Service Bus client library when a new message arrives in the queue.
    */
  override def onMessageAsync(message: IMessage): CompletableFuture[Void] = {
    val messageStr = new String(message.getBody, "UTF-8")
    val json = parse(messageStr)
    val config = json.extractOpt[Config]

    if (config.isDefined) {
      // Block for up to two minutes for a Spark thread to acknowledge the updated
      // state.
      //
      // Throws if timeout is exceeded, which will unlock message without deleting. This may cause reordering of configs
      // in the Service Bus queue, so in production code, you may wish to include a timestamp etc. on data.
      if (!nextConfig.offer(config.get, 2, TimeUnit.MINUTES)) {
        throw new Exception("No Spark thread acknowledged the update message within the timeout.")
      }
    }

    CompletableFuture.completedFuture(null)
  }
}