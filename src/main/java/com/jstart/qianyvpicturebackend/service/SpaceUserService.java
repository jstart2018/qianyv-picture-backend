package com.jstart.qianyvpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jstart.qianyvpicturebackend.model.dto.spaceUser.SpaceUserAddRequest;
import com.jstart.qianyvpicturebackend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.jstart.qianyvpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jstart.qianyvpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 28435
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-03-05 20:05:27
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员关系数据
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验参数
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 构建查询条件
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取单个空间用户关系信息
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间用户关系信息集合
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
