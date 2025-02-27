package com.jstart.qianyvpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureUploadRequest;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 28435
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-24 09:26:44
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param multipartFile 文件
     * @param pictureUploadRequest 上传请求参数，只有一个id属性，id不为空表示更新图片，否则为新增
     * @param loginUser 当前登录的用户
     * @return 脱敏后的图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * 构建Picture的查询条件
     * @param pictureQueryRequest   查询请求实体
     * @return 返回查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片的封装
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取分页图片封装
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 校验图片数据，更新时使用
     * @param picture
     */
    public void validPicture(Picture picture);
}
