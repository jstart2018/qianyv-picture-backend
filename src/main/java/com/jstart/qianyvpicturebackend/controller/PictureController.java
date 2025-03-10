package com.jstart.qianyvpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.auth.Constant.SpaceUserPermissionConstant;
import com.jstart.qianyvpicturebackend.auth.Manager.SpaceUserAuthManager;
import com.jstart.qianyvpicturebackend.auth.StpKit;
import com.jstart.qianyvpicturebackend.auth.annotation.SaSpaceCheckPermission;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.entity.DeleteRequest;
import com.jstart.qianyvpicturebackend.common.entity.Result;
import com.jstart.qianyvpicturebackend.common.enums.PictureStatusEnum;
import com.jstart.qianyvpicturebackend.common.manager.CosManager;
import com.jstart.qianyvpicturebackend.common.manager.FileManager;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.file.UploadPictureResult;
import com.jstart.qianyvpicturebackend.model.dto.picture.*;
import com.jstart.qianyvpicturebackend.model.dto.space.SpaceQueryRequest;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.PictureTagCategory;
import com.jstart.qianyvpicturebackend.model.vo.PictureVO;
import com.jstart.qianyvpicturebackend.service.PictureService;
import com.jstart.qianyvpicturebackend.service.SpaceService;
import com.jstart.qianyvpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture/")
@Slf4j
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public Result<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return Result.success(pictureVO);
    }

    /**
     * 上传图片（可重新上传）-url上传
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public Result<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return Result.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest 删除请求参数（只有一个id）
     * @param request       httpServletRequest
     * @return 返回布尔类型
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public Result<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                         HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorEnum.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.deletePicture(deleteRequest, loginUser);
        return Result.success(result);
    }

    /**
     * 更新图片，仅管理员可用
     *
     * @param pictureUpdateRequest 更新图片请求
     * @param request              HttpServletRequest
     * @return 返回布尔类型
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                         HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorEnum.PARAMS_ERROR);
        //转移实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //校验参数合理性
        pictureService.validPicture(picture);
        //图片审核数据处理：
        pictureService.pictureReviewPretreatment(picture, userService.getLoginUser(request));
        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorEnum.SYSTEM_ERROR, "操作数据库失败");
        return Result.success(result);
    }

    /**
     * 编辑图片，用户专用
     *
     * @param pictureEditRequest 编辑请求
     * @param request            request
     * @return 布尔
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public Result<Boolean> editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.editPicture(pictureEditRequest, loginUser);

        return Result.success(result);
    }

    /**
     * 根据id获取图片，未脱敏，仅管理员可用
     *
     * @param id id
     * @return 未脱敏图片数据
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Picture> getPictureById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorEnum.PARAMS_ERROR);
        //查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture != null, ErrorEnum.NOT_FOUND_ERROR);
        return Result.success(picture);

    }

    /**
     * 根据id获取图片，已脱敏，用户专用
     *
     * @param id id
     * @return 脱敏图片数据
     */
    @PostMapping("/get/vo")
    public Result<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorEnum.PARAMS_ERROR);
        //查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture != null, ErrorEnum.NOT_FOUND_ERROR);
        //查是否有权限
        Space space = null;
        if (picture.getSpaceId() != null) {
            //用sa-token鉴权
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(picture, loginUser);

            //空间里面的图片要有读权限
            boolean result = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(result, ErrorEnum.NO_AUTH_ERROR);

            space = spaceService.getById(picture.getSpaceId());
            ThrowUtils.throwIf(space==null, ErrorEnum.NOT_FOUND_ERROR);
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);

        //塞入当前用户对该图片拥有的权限：
        List<String> permissionList = spaceUserAuthManager
                .getPermissionList(space, userService.getLoginUser(request));

        pictureVO.setPermissionList(permissionList);

        return Result.success(pictureVO);
    }


    /**
     * 分页查询图片，管理员专用
     *
     * @param pictureQueryRequest 分页请求dto
     * @return 分页内容
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<Picture>> getPictureList(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorEnum.PARAMS_ERROR);
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();

        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return Result.success(picturePage);
    }

    /**
     * 分页查询图片，用户用
     *
     * @param pictureQueryRequest 分页请求dto
     * @return 脱敏后 分页内容
     */
    @PostMapping("/list/page/vo")
    public Result<Page<PictureVO>> getPictureListVO(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorEnum.PARAMS_ERROR);
        //防止爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorEnum.PARAMS_ERROR, "页面数量过大");

        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureQueryRequest, picture);
        //判断是看公共图库还是私有图库
        //如果是查个人空间图片,空间id不为空
        if (pictureQueryRequest.getSpaceId() != null ) {

            User loginUser = userService.getLoginUser(request);//这里同时判断了登录

            Long spaceId = pictureQueryRequest.getSpaceId();
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorEnum.NOT_FOUND_ERROR, "没有该空间");
            /*if (!(space.getUserId().equals(loginUser.getId()))) {
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR);
            }*/

            //使用sa-token鉴权：
            //空间里面的图片要有读权限
            boolean result = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(result, ErrorEnum.NO_AUTH_ERROR);
            pictureQueryRequest.setNullSpace(false);
        } else {
            //查公共图片的请求，默认只能看已经过审的数据
            pictureQueryRequest.setReviewStatus(PictureStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpace(true);
        }

        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.PicturePageToVOPage(picturePage);
        return Result.success(pictureVOPage);

    }

    /**
     * 分页查询图片，用户用，查询缓存的
     *
     * @param pictureQueryRequest 分页请求dto
     * @return 脱敏后 分页内容
     */
    @PostMapping("/list/page/vo/cache")
    public Result<Page<PictureVO>> getPictureListVOWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorEnum.PARAMS_ERROR);
        //防止爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorEnum.PARAMS_ERROR, "页面数量过大");

        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(pictureQueryRequest);

        return Result.success(pictureVOPage);

    }

    /**
     * 获取预标签信息
     *
     * @return
     */
    @GetMapping("/tag_category")
    public Result<PictureTagCategory> getPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();

        List<String> tags = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "创意");
        List<String> categories = Arrays.asList("表情包", "电商", "海报", "素材", "人文", "景观", "AI");
        pictureTagCategory.setTagList(tags);
        pictureTagCategory.setCategoryList(categories);
        return Result.success(pictureTagCategory);
    }


    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorEnum.PARAMS_ERROR);
        Boolean isOk = pictureService.doPictureReview(pictureReviewRequest, request);
        return Result.success(isOk);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorEnum.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        ThrowUtils.throwIf(uploadCount <= 0, ErrorEnum.OPERATION_ERROR, "图片内部解析错误");
        return Result.success(uploadCount);
    }


}
