package com.jstart.qianyvpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureReviewRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureUploadRequest;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 28435
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-24 09:26:44
*/
public interface PictureService extends IService<Picture> {

    /**
     * 文件操作 审核预处理，如果是管理员默认审核通过，普通用户默认设为待审核
     * @param picture
     * @param loginUser
     */
    void pictureReviewPretreatment(Picture picture, User loginUser);

    /**
     * 上传图片
     * @param inputSource 文件
     * @param pictureUploadRequest 上传请求参数，只有一个id属性，id不为空表示更新图片，否则为新增
     * @param loginUser 当前登录的用户
     * @return 脱敏后的图片信息
     */
    PictureVO uploadPicture(Object inputSource,
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
    Page<PictureVO> PicturePageToVOPage(Page<Picture> picturePage);

    /**
     * 校验图片数据，更新时使用
     * @param picture
     */
    public void validPicture(Picture picture);

    /**
     * 审核图片
     * @param pictureReviewRequest 审核内容
     * @param request 用于获取登录用户
     * @return 成功与否
     */
    Boolean doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 获取分页数据 VO的
     * @param pictureQueryRequest
     * @return
     */
    Page<PictureVO> getPictureVOPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 清理图片COS中的对象：
     */
    @Async
    void clearPictureFile(Picture oldPicture);
}
