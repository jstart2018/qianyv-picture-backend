package com.jstart.qianyvpicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceAddRequest;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceQueryRequest;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 28435
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-03 13:21:23
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验图片数据，更新时使用
     * @param space
     * @param add
     */
    public void validSpace(Space space,boolean add);

    /**
     * 构建Space的查询条件
     * @param spaceQueryRequest   查询请求实体
     * @return 返回查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取单个图片的封装
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 根据空间级别填充空间容量
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    long createSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);
}
