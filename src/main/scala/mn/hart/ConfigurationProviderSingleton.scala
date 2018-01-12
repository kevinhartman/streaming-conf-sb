package mn.hart

import java.util.concurrent.CompletableFuture

import com.microsoft.azure.servicebus.{ExceptionPhase, IMessage, IMessageHandler}

object ConfigurationProviderSingleton {


  object ConfigurationMessageHandler extends IMessageHandler {
    override def notifyException(exception: Throwable, phase: ExceptionPhase): Unit = {
      sys.error(exception.toString)
    }

    override def onMessageAsync(message: IMessage): CompletableFuture[Void] = {

    }
  }
}
