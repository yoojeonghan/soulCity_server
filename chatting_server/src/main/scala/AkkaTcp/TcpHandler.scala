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

// 트리 구조의 최상층에서 모든 액터들을 관리하는 슈퍼바이저 액터 클래스
class TcpSupervisor extends Actor with ActorLogging
{  
  override def supervisorStrategy = OneForOneStrategy(loggingEnabled = true)
  {
    case NonFatal(_) ⇒ Stop
  }

  override def receive : Actor.Receive =
  {
    case Tcp.CommandFailed(_: Tcp.Bind) ⇒ context stop self
      println("[KelsaikNest_ChattingServer] CommandFailded!")
    
    case b@Tcp.Bound(localAddress) ⇒
      println("[KelsaikNest_ChattingServer] ready and listening for new connections on" + localAddress)
      
      val listener = sender()
      listener ! Tcp.ResumeAccepting(1)
      context.become(listening(listener))
  }

  def listening(listener: ActorRef): Receive =
  {
    case c@Tcp.Connected(remote, local) =>
      
      val connection = sender()
      val handler = context.actorOf(Props(classOf[TcpHandler], connection))
      println("[KelsaikNest_ChattingServer] Client Connect to Actor. Client Address is" + handler)

      connection ! Tcp.Register(handler)
      listener ! Tcp.ResumeAccepting(1)
      
      Server.UserActorList += connection      
      println("[KelsaikNest_ChattingServer] Connect User Num is " + Server.UserActorList.size)
  }
}

object TcpHandler
{
  case object Ack extends Tcp.Event
}

// 각 연결에 대하여 생성되는 TcpHandler 액터
class TcpHandler(connection: ActorRef) extends Actor with ActorLogging
{
  import TcpHandler._
  import akka.io.Tcp._

  override def preStart(): Unit =
  {
    println("[KelsaikNest_ChattingServer] Connect Clinet's Tcp I/O Actor Create. This Actor Address is "+self.path.address)
    context watch connection
    
    connection ! ResumeReading
  }

  def receive: Receive =
  {
    case ConfirmedClosed | Terminated(_) ⇒
      context.stop(self)
      
    case PeerClosed ⇒ println("[KelsaikNest_ChattingServer] Peer Closed.")
      connection ! ConfirmedClose

    case e@Received(data) ⇒

          val temp = new String(data.toArray, "UTF-8")
          val result = JSON.parseFull(temp)
          
          println("[KelsaikNest_ChattingServer] Server response data : "+result)
    
          result match
          {
            case Some(e) ⇒ 
              val globalMap = result.get.asInstanceOf[Map[String, Any]]
              val RequestNum = globalMap.get("RequestNum").get.asInstanceOf[String]
                        
            RequestNum match
            {
            case "2002"⇒
              println("[KelsaikNest_ChattingServer] 2002 Received")
              val UserList = globalMap.get("UserList").get.asInstanceOf[Array[String]]
              println(UserList.toString())
              
            case "900" ⇒
              val RequestUser = globalMap.get("RequestUser").get.asInstanceOf[String]
              val AnswerString = Server.JsonConverter.toJson(Map("AnswerNum"->RequestNum ,"RequestUser"-> RequestUser))
              val AnswerByteString = ByteString.apply(AnswerString, StandardCharsets.UTF_8)

              for(UserActor <- Server.UserActorList)
              {
                   UserActor ! Write(AnswerByteString, Ack)
              }  
                 
            case "1000" ⇒ // 일반채팅
              val RequestUser = globalMap.get("RequestUser").get.asInstanceOf[String]
              val RequestString = globalMap.get("RequestString").get.asInstanceOf[String]

              val AnswerString = Server.JsonConverter.toJson(Map("AnswerNum"->RequestNum ,"RequestUser"-> RequestUser, "AnswerString"->RequestString)) 
              val AnswerString2 = new String(AnswerString.getBytes(), "UTF-8")
              val AnswerByteString = ByteString.apply(AnswerString2, StandardCharsets.UTF_8)
              
              for(UserActor <- Server.UserActorList)
              {
                   UserActor ! Write(AnswerByteString, Ack)
              }    
                 println("[KelsaikNest_ChattingServer] Server sending data : "+AnswerString2)
                 val AnswerStringResult = new String(AnswerByteString.toArray, "UTF-8")
                 println("[KelsaikNest_ChattingServer] Server sending data : "+AnswerStringResult)

                // 전체채팅
            case "1001" ⇒
              
                // 파티채팅
            case "1002" ⇒
              
                // 길드채팅
            case "1003" ⇒
              
                // 귓속말
            case "1004" ⇒
              
                // 접속종료
            case "2000" ⇒               
                 val RequestUser = globalMap.get("RequestUser").get.asInstanceOf[String]
                 val AnswerString = Server.JsonConverter.toJson(Map("AnswerNum"->RequestNum ,"RequestUser"-> RequestUser))
                 val AnswerByteString = ByteString(AnswerString, "UTF-8")

                 for(UserActor <- Server.UserActorList)
                 {
                   if(!UserActor.equals(connection))
                   {
                       UserActor ! Write(AnswerByteString, Ack)
                   }
                 }       
                 println("[KelsaikNest_ChattingServer] Server sending data : "+AnswerByteString.map(_.toChar).mkString)
               
                 connection ! ConfirmedClose
                
                // 유저리스트에서 지우고
                Server.UserActorList -= connection
                println("[KelsaikNest_ChattingServer] Connect User Num is " + Server.UserActorList.size)
            
                // 액터 중지
                context.stop(self)

            }         
            case None ⇒              
              println("[KelsaikNest_ChattingServer] Server response data fail")
          }
          
      context.become(
        {
          // 긍정 응답 메시지
        case Ack ⇒
          connection ! ResumeReading
          context.unbecome()

        case WritingResumed ⇒ connection ! Write(data, Ack)

        case Tcp.CommandFailed(Write(_, _)) =>
          connection ! ResumeWriting

        },
        discardOld = false)
  }
  
  
  
}
