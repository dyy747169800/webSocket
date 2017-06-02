package com.uban.webSocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 */
@ServerEndpoint("/websocket")
public class WebSocketTest {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    private static final List<String> historyMessageArray = new ArrayList<String>();

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static CopyOnWriteArraySet<WebSocketTest> webSocketSet = new CopyOnWriteArraySet<WebSocketTest>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(Session session) throws UnsupportedEncodingException {
        this.session = session;
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        String username = getRequestParamMapBySession(session).get("nickname");
        System.out.println(username +"加入群聊！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) throws UnsupportedEncodingException {
        webSocketSet.remove(this);  //从set中删除
        String nickname = getRequestParamMapBySession(session).get("nickname");
        subOnlineCount();           //在线数减1
        System.out.println(nickname + "退出了群聊！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        if("loadHistoryMessage".equals(message)){
            for (String historyMessage : historyMessageArray) {
                    this.sendMessage(historyMessage);
            }
        }else {
            Map<String, String> requestParam = getRequestParamMapBySession(session);
            String nickname = requestParam.get("nickname");
            historyMessageArray.add(nickname+":"+message);
            System.out.println("来自客户端的消息:" + message);
            //群发消息
            for (WebSocketTest item : webSocketSet) {
                item.sendMessage(nickname + ":" + message);
            }
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     *
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketTest.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketTest.onlineCount--;
    }

    private  static Map<String,String> getRequestParamMapBySession(Session session) throws UnsupportedEncodingException {
        String queryString = session.getQueryString();
        if("".equals(queryString) || null == queryString){
            return null;
        }else {
            Map<String,String> paramsMap = new HashMap();
            //(i & 1)
            //两个只要有一个是偶数就为等于0
            //两个都是奇数等于1
            String[] queryParamArray = queryString.split("=");
            for (int i = 0; i < queryParamArray.length; i++) {
                if((i & 1) == 0){
                    //偶数
                    paramsMap.put(queryParamArray[i], URLDecoder.decode(queryParamArray[i+1],"utf-8"));
                }
            }
            return paramsMap;
        }


    }



}