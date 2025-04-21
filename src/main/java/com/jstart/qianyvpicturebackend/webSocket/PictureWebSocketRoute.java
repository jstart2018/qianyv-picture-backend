package com.jstart.qianyvpicturebackend.webSocket;

import com.jstart.qianyvpicturebackend.webSocket.handle.PictureEditHandle;
import com.jstart.qianyvpicturebackend.webSocket.interceptor.WsHandshakeInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Component
@EnableWebSocket //启用webSocket的重要注解
public class PictureWebSocketRoute implements WebSocketConfigurer {

    @Resource
    private PictureEditHandle pictureEditHandle;

    @Resource
    private WsHandshakeInterceptor wsHandshakeInterceptor;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandle,"/ws/picture/edit")
                .addInterceptors(wsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
