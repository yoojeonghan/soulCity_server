package AkkaTcp

import TcpHandler.Ack
import akka.actor.ProviderSelection.Cluster
import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import akka.io.Tcp
import akka.io.Tcp.{Connect, Received, Write, WritingResumed}
import akka.util._
import akka.io.{IO, Tcp}

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal
import scala.util.parsing.json._
import scala.collection.mutable.ListBuffer
import java.nio.charset.{ Charset, StandardCharsets }
import scala.reflect.ClassTag
import scala.collection.IndexedSeqOptimized

class ClientSupervisor extends Actor with ActorLogging
{
  override def supervisorStrategy = OneForOneStrategy(loggingEnabled = true)
  {
    case NonFatal(_) ⇒ Stop
  }
  
  override def receive : Actor.Receive =
  {
      case Tcp.CommandFailed(_: Connect) ⇒ context stop self
              println("[KelsaikNest_ChattingServer] Matting Server Command Failded!")

      case c @ Tcp.Connected(remote, local) ⇒
              val connection = sender()
              connection ! Tcp.Register(self)
              println("[KelsaikNest_ChattingServer] Matting Server Connection Success!")
              
              val RequestString = Server.JsonConverter.toJson(Map("RequestNum"->"102"))
              val RequestByteString = ByteString.apply(RequestString, StandardCharsets.UTF_8)
              
              connection ! Tcp.Write(RequestByteString)
              
              val listener = sender()
              listener ! Tcp.ResumeAccepting(1)
              
              context.become(listening(listener))
  }
  
  def listening(listener : ActorRef) : Receive = 
  {
      case data: ByteString ⇒ //connection ! Write(data)
               println("[KelsaikNest_ChattingServer] Become Message from Matching Server")
                      
      case Tcp.CommandFailed(w: Write) ⇒
               println("[KelsaikNest_ChattingServer] Matting Server Command Failded!")
        
      case Received(data) ⇒
               val temp = new String(data.toArray, "UTF-8")
               val result = JSON.parseFull(temp)
               
               result match
               {
                    case Some(e) ⇒
                       val globalMap = result.get.asInstanceOf[Map[String, Any]]
                       val RequestNum = globalMap.get("AnswerNum").get.asInstanceOf[String]    
                    
                    RequestNum match
                    {
                      case "100" ⇒
                         println("[KelsaikNest_ChattingServer] Become Connect Success Message from Matching Server")
                         
                      case "2002" ⇒
                         println("[KelsaikNest_ChattingServer] 2002 Received")
                         val UserList = globalMap.get("UserList")
                         val Dungeon = ListBuffer[ActorRef]()
                    }
                    
                    case None ⇒
                       println("[KelsaikNest_ChattingServer] Data fail from Matching Server")
               }

      case "close" ⇒ 
               val connection = sender()
               connection ! Tcp.Close
               println("[KelsaikNest_ChattingServer] Close Connection Matting Server")
                
      case _: Tcp.ConnectionClosed ⇒ context stop self
               println("[KelsaikNest_ChattingServer] Connection Closed to Matting Server")  
  }
}

class ClientHandler 
{
  
}