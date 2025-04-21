package com.jstart.qianyvpicturebackend.webSocket.interceptor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.enums.SpaceRoleEnum;
import com.jstart.qianyvpicturebackend.common.enums.SpaceTypeEnum;
import com.jstart.qianyvpicturebackend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.entity.SpaceUser;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.service.PictureService;
import com.jstart.qianyvpicturebackend.service.SpaceService;
import com.jstart.qianyvpicturebackend.service.SpaceUserService;
import com.jstart.qianyvpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;


    //握手前拦截逻辑
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //获取当前登录用户
        User loginUser = null;
        HttpServletRequest httpServletRequest = null;
        if (request instanceof ServletServerHttpRequest) {
            httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            loginUser = userService.getLoginUser(httpServletRequest);
        }
        //校验该用户是否有编辑权限
        assert httpServletRequest != null;
        String pictureId = httpServletRequest.getParameter("pictureId");
        if (StrUtil.isBlank(pictureId)) {
            log.error("参数错误，pictureId不能为空");
            return false;
        }
        Picture picture = pictureService.getById(pictureId);
        if (picture == null) {
            log.error("没有该图片");
            return false;
        }
        // 要是团队空间，并且有编辑者权限
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            log.error("该图片为公共空间图片,为开放编辑");
            return false;
        }
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            log.error("系统错误，找到该图片的空间");
            return false;
        }
        if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())){
            log.error("私有空间未开放协同编辑");
            return false;
        }
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setSpaceId(spaceId);
        spaceUserQueryRequest.setUserId(loginUser.getId());
        QueryWrapper<SpaceUser> spaceUserQueryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserQueryWrapper);
        if (spaceUser == null && !spaceUser.getSpaceRole().equals(SpaceRoleEnum.EDITOR.getValue())
                && !spaceUser.getSpaceRole().equals(SpaceRoleEnum.ADMIN.getValue())){
            log.error("用户无权限");
            return false;
        }
        //设置用户的登录信息到webSocket的会话当中
        attributes.put("user", loginUser);
        attributes.put("picture", picture);
        return true;
    }

    //握手后拦截逻辑
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
