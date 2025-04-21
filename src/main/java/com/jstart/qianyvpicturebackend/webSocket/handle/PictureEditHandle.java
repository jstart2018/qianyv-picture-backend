package com.jstart.qianyvpicturebackend.webSocket.handle;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.service.UserService;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditMessageTypeEnum;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditRequestMessage;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureEditHandle extends TextWebSocketHandler {

    @Resource
    private UserService userService;
    
    //正在编辑图片的用户,key:pictureId、value:userId
    public static final ConcurrentHashMap<Long,Long> userEditing = new ConcurrentHashMap<>();

    //各个图片的编辑室 key:pictureId、value:当前图片编辑室的用户session
    public static final ConcurrentHashMap<Long, Set<WebSocketSession>> pictureEditRome = new ConcurrentHashMap<>();


    /**
     * 连接建立时，加入编辑室，并广播给所有用户
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //给当前用户加入编辑室
        User user = (User) session.getAttributes().get("user");
        Picture picture = (Picture) session.getAttributes().get("picture");
        //(1)如果之前没有人进入该图片编辑，先初始化该图片编辑室的空set集合
        pictureEditRome.putIfAbsent(picture.getId(), ConcurrentHashMap.newKeySet());
        //(2)将该用户加入该编辑室
        pictureEditRome.get(picture.getId()).add(session);
        //(3)构造响应消息
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        responseMessage.setMessage(String.format("%s 进入编辑室",user.getUserName()));
        responseMessage.setUser(userService.userToVO(user));
        //(4)广播给其他用户
        broadcastPictureToAll(null,responseMessage,picture);

        super.afterConnectionEstablished(session);

    }

    /**
     * 客户端给服务端发消息时
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Picture picture = (Picture) session.getAttributes().get("picture");
        //获取前端发过来的消息
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        //解析前端发过来的是什么请求，并广播该标记是的其他用户
        String type = requestMessage.getType();
        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);

        //TODO 策略模式实现不同的动作发起消息
        switch (enumByValue){
            case ENTER_EDIT://开始编辑
                    break;
            case EXIT_EDIT://执行了编辑动作
                System.out.println("1123");
            case EDIT_ACTION://退出编辑状态

            default:
                log.error("请求参数错误：操作图片动作错误");

        }


        super.handleTextMessage(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    public void broadcastPictureToAll(WebSocketSession excludeSession, PictureEditResponseMessage responseMessage, Picture picture) throws IOException {
        //构造广播消息
        ObjectMapper objectMapper = new ObjectMapper();
        //精度修复
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module); //自定义objectMapper转换规则，解决long类型在前端精度丢失问题
        String respMessageJson = objectMapper.writeValueAsString(responseMessage);
        TextMessage textMessage = new TextMessage(respMessageJson);
        //在该图片编辑室广播消息
        Set<WebSocketSession> sessions = pictureEditRome.get(picture.getId());
        for (WebSocketSession session : sessions) {
            //如果是要排除的session，那就不用对这个session广播消息
            if (excludeSession != null && excludeSession.equals(session)) {
                continue;
            }
            //如果这个session是开启的才发送
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }

        }

    }


}
