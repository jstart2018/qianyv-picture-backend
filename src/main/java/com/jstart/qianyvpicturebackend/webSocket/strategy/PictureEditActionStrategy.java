package com.jstart.qianyvpicturebackend.webSocket.strategy;

import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditActionEnum;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditMessageTypeEnum;
import com.jstart.qianyvpicturebackend.webSocket.model.PictureEditResponseMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface PictureEditActionStrategy {

    PictureEditMessageTypeEnum getPictureEditActionEnum();

    void executeStrategy(ConcurrentHashMap<Long, Set<WebSocketSession>> pictureEditRome, WebSocketSession excludeSession, PictureEditResponseMessage responseMessage, Picture picture);

}
