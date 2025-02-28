package com.jstart.qianyvpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jstart.qianyvpicturebackend.common.enums.PictureStatusEnum;
import com.jstart.qianyvpicturebackend.common.manager.FileManager;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.FilePictureUpload;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.PictureUploadTemplate;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.UrlPictureUpload;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.file.UploadPictureResult;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureQueryRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureReviewRequest;
import com.jstart.qianyvpicturebackend.model.dto.picture.PictureUploadRequest;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.PictureVO;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;
import com.jstart.qianyvpicturebackend.service.PictureService;
import com.jstart.qianyvpicturebackend.mapper.PictureMapper;
import com.jstart.qianyvpicturebackend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 28435
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-24 09:26:44
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private PictureMapper pictureMapper;
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 文件操作 审核预处理，如果是管理员默认审核通过，普通用户默认设为待审核
     * @param picture
     * @param loginUser
     */
    @Override
    public void pictureReviewPretreatment(Picture picture, User loginUser){
        if (userService.isAdmin(loginUser)){
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动审核通过");
            picture.setReviewTime(new Date());
        }else
            picture.setReviewStatus(PictureStatusEnum.REVIEWING.getValue());
    }


    /**
     * 上传图片
     *
     * @param inputSource        文件
     * @param pictureUploadRequest 上传请求参数，只有一个id属性，id不为空表示更新图片，否则为新增
     * @param loginUser            当前登录的用户
     * @return 脱敏后的图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorEnum.NO_AUTH_ERROR);
        //1、 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture picture = this.getById(pictureId);
            ThrowUtils.throwIf(picture==null,ErrorEnum.NOT_FOUND_ERROR, "图片不存在");
            //且仅有本人或管理员才能更新图片信息（重新上传）
            if (picture.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR);
            }
        }
        // 上传图片，得到信息
        // 2、按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        //3、判断是文件上传的还是url上传的
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 4、构造要入库的图片信息
        Picture picture = new Picture();
        //图片审核数据处理：
        this.pictureReviewPretreatment(picture,loginUser);
        //基础数据
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 5、如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //6、操作数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorEnum.OPERATION_ERROR, "图片上传数据库失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 构建Picture的查询条件
     * @param pictureQueryRequest   查询请求实体
     * @return 返回查询条件
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        //从请求实体中获取 要查询的数据，一个一个塞
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        //从多字段里模糊搜索
        if (!StringUtils.isBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name",searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        queryWrapper.like(StrUtil.isNotBlank(name),"name",name);
        queryWrapper.like(StrUtil.isNotBlank(introduction),"introduction",introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category),"category",category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize),"picSize",picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth),"picWidth",picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight),"picHeight",picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale),"picScale",picScale);
        queryWrapper.like(StrUtil.isNotBlank(picFormat),"picFormat",picFormat);
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId",userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        if (CollUtil.isNotEmpty(tags)){
            for (String tag : tags) {
                queryWrapper.like("tags","\""+tag+"\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片的封装（实体转VO）
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取分页图片封装(分页实体转分页VO)
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();

        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        /**
         * User::getId 是一个方法引用，表示根据 User 对象的 getId() 方法返回的 ID 对用户进行分组。
         * groupingBy() 会生成一个 Map，其中每个键是用户的 ID，值是该 ID 对应的 User 对象的列表。
         * 例如，如果用户 ID 为 1 的用户有多个，返回的 Map<Long, List<User>> 中，
         * 键为 1 的值将是一个包含所有 ID 为 1 的用户对象的列表。
         */
        Map<Long, List<User>> userIdUserListMap = userService
                .listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }else
                throw new BusinessException(ErrorEnum.SYSTEM_ERROR,"picture转VO错误");
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 校验图片数据，更新时使用
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorEnum.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorEnum.PARAMS_ERROR, "id 不能为空");
        //查询该图片是否存在
        Picture p = pictureMapper.selectById(id);
        ThrowUtils.throwIf(p==null,ErrorEnum.NOT_FOUND_ERROR,"图片不存在");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorEnum.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorEnum.PARAMS_ERROR, "简介过长");
        }
    }


    @Override
    public Boolean doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request){
        //参数校验
        //审核选项合法（0、1、2）
        Long id = pictureReviewRequest.getId();
        int reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureStatusEnum pictureStatusEnum = PictureStatusEnum.getByValue(reviewStatus);
        ThrowUtils.throwIf(id==null||reviewMessage==null||pictureStatusEnum==null,ErrorEnum.PARAMS_ERROR);
        //查询是否有对应图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorEnum.NOT_FOUND_ERROR);
        //业务逻辑：
        //不能反复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorEnum.OPERATION_ERROR,"不能反复审核");
        }
        //更新审核状态
        User loginUser = userService.getLoginUser(request);
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest,picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        picture.setReviewStatus(reviewStatus);
        picture.setReviewMessage(reviewMessage);

        //操作数据库返回
        return this.updateById(picture);
    }


}




