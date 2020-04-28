package AkkaTcp

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.actor._
import scala.collection.mutable.ListBuffer

object Server extends App
{
  // 액터를 담는 컨테이너인 액터시스템 생성. 모든 액터는 액터시스템 내부에서 동작을 수행한다.
  implicit val system = ActorSystem()

  // 트리 구조의 최상층에서 모든 액터들을 관리하는 슈퍼바이저 액터 생성

  val tcpService = system.actorOf(Props[TcpSupervisor])
  val tcpClient = system.actorOf(Props[ClientSupervisor])

  // TCP서버를 만들고 인바운드 연결 수신 대기를 위해 Tcp관리자에게 Bind명령 지시.
  // 슈퍼바이저 액터 tcpService에게 IP와 포트번호를 보냄.

  IO(Tcp).tell(Tcp.Bind(tcpService, new InetSocketAddress("172.31.20.245", 801), options = Nil, pullMode = true), tcpService)
  IO(Tcp).tell(Tcp.Connect(new InetSocketAddress("52.78.90.90", 802)), tcpClient)
  
  //IO(Tcp) ! Tcp.Connect(new InetSocketAddress("52.78.90.90", 802))
  
  val UserActorList = ListBuffer[ActorRef]()
  val DungeonActorList = ListBuffer[ListBuffer[ActorRef]]()
  
  // JSON 컨버터
  object JsonConverter 
  {
  	def toJson(o: Any) : String = {
      var json = new ListBuffer[String]()
      o match {
        case m: Map[_,_] => {
          for ( (k,v) <- m ) {
            var key = escape(k.asInstanceOf[String])
            v match {
              case a: Map[_,_] => json += "\"" + key + "\":" + toJson(a)
              case a: List[_] => json += "\"" + key + "\":" + toJson(a)
              case a: Int => json += "\"" + key + "\":" + a
              case a: Boolean => json += "\"" + key + "\":" + a
              case a: String => json += "\"" + key + "\":\"" + escape(a) + "\""
              case _ => ;
            }
          }
        }
        case m: List[_] => {
          var list = new ListBuffer[String]()
          for ( el <- m ) {
            el match {
              case a: Map[_,_] => list += toJson(a)
              case a: List[_] => list += toJson(a)
              case a: Int => list += a.toString()
              case a: Boolean => list += a.toString()
              case a: String => list += "\"" + escape(a) + "\""
              case _ => ;
            }
          }
          return "[" + list.mkString(",") + "]"
        }
        case _ => ;
      }
      return "{" + json.mkString(",") + "}"
    }
  
    private def escape(s: String) : String = {
      return s.replaceAll("\"" , "\\\\\"");
  }
  }
  
  // ByteString->UTF8 String 컨버터
  def convert(bs: List[Byte]) : List[String] = 
  {
    bs match 
    {
      case count_b1 :: count_b2 :: t =>
        val count =  ((count_b1 & 0xff) << 8) | (count_b2 & 0xff)
        val (chars, leftover) = t.splitAt(count)
        new String(chars.toArray, "UTF-8") :: convert(leftover)
      
      case _ => List()
    }
  }
}