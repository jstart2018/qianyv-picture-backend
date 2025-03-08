package com.jstart.qianyvpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.entity.DeleteRequest;
import com.jstart.qianyvpicturebackend.common.enums.PictureStatusEnum;
import com.jstart.qianyvpicturebackend.common.manager.CosManager;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.FilePictureUpload;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.PictureUploadTemplate;
import com.jstart.qianyvpicturebackend.common.manager.uploadFile.UrlPictureUpload;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.file.UploadPictureResult;
import com.jstart.qianyvpicturebackend.model.dto.picture.*;
import com.jstart.qianyvpicturebackend.model.entity.Picture;
import com.jstart.qianyvpicturebackend.model.entity.Space;
import com.jstart.qianyvpicturebackend.model.entity.User;
import com.jstart.qianyvpicturebackend.model.vo.PictureVO;
import com.jstart.qianyvpicturebackend.model.vo.UserVO;
import com.jstart.qianyvpicturebackend.service.PictureService;
import com.jstart.qianyvpicturebackend.mapper.PictureMapper;
import com.jstart.qianyvpicturebackend.service.SpaceService;
import com.jstart.qianyvpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 28435
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-24 09:26:44
 */
@Service
@Slf4j
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
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;

    //本地缓存配置
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024) //初始容量
                    .maximumSize(10000L) //最大容量
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 文件操作 审核预处理，如果是管理员默认审核通过，普通用户默认设为待审核
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void pictureReviewPretreatment(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动审核通过");
            picture.setReviewTime(new Date());
        } else
            picture.setReviewStatus(PictureStatusEnum.REVIEWING.getValue());
    }


    /**
     * 上传图片
     *
     * @param inputSource          文件
     * @param pictureUploadRequest 上传请求参数，只有一个id属性，id不为空表示更新图片，否则为新增
     * @param loginUser            当前登录的用户
     * @return 脱敏后的图片信息
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorEnum.NO_AUTH_ERROR);
        //1、 判断是否是上传到非公共空间，只要空间拥有者才可以上传
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorEnum.NOT_FOUND_ERROR,"空间错误/");
            if (!loginUser.getId().equals(space.getUserId()))
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR,"没有该空间权限");
            //校验空间大小
            if (space.getTotalCount()>=space.getMaxCount())
                throw new BusinessException(ErrorEnum.OPERATION_ERROR,"空间可用条数不足");
            if (space.getTotalSize() >= space.getMaxSize())
                throw new BusinessException(ErrorEnum.OPERATION_ERROR,"空间可用容量不足");
        }
        //2、用于判断是新增还是更新图片
        Long pictureId = null;

        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture picture = this.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorEnum.NOT_FOUND_ERROR, "图片不存在");
            //且仅有本人或管理员才能更新图片信息（重新上传）
            if (picture.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR);
            }
            //是更新，且有权限，先删除原来
            this.clearPictureFile(picture,spaceId);

        }

        // 上传图片，得到信息
        // 1、区分是上传到哪个空间
        String uploadPathPrefix;
        if (spaceId == null){
            //上传到公共空间
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else {
            //上传到私有空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //2、判断是文件上传的还是url上传的
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        //1、图片审核情况填充：
        this.pictureReviewPretreatment(picture, loginUser);
        //2、基础数据
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        //3、名字从request中传来
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        // 4、如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        transactionTemplate.execute(status -> {
            //、操作数据库
            boolean saveResult = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!saveResult, ErrorEnum.OPERATION_ERROR, "图片上传数据库失败");
            if (spaceId == null)
                return saveResult;
            boolean updateResult = spaceService.lambdaUpdate().eq(Space::getId, spaceId)
                    .setSql("totalSize = totalSize+" + picture.getPicSize())
                    .setSql("totalCount = totalCount+ 1")
                    .update();
            ThrowUtils.throwIf(!updateResult, ErrorEnum.OPERATION_ERROR, "空间更新失败");

            return saveResult && updateResult;
        });
        //更新空间大小
        return PictureVO.objToVo(picture);
    }

    @Override
    public boolean deletePicture(DeleteRequest deleteRequest, User loginUser) {
        Long id = deleteRequest.getId();
        //查询数据库数据是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture==null, ErrorEnum.NOT_FOUND_ERROR,"没有该图片");
        //校验图片权限
        //已经用注解校验权限
        //this.checkPictureAuth(picture,loginUser);

        this.clearPictureFile(picture,picture.getSpaceId());

        transactionTemplate.execute(status -> {
            //如果是删除 个人或者团队空间的图片，释放额度
            Space space = spaceService.getById(picture.getSpaceId());
            if (space != null) {
                boolean updateResult = spaceService.lambdaUpdate().eq(Space::getId, space.getId())
                        .setSql("totalSize = totalSize-" + picture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!updateResult, ErrorEnum.OPERATION_ERROR, "空间更新失败");
            }
            //删除数据库数据
            boolean removeResult = this.removeById(id);
            ThrowUtils.throwIf(!removeResult, ErrorEnum.OPERATION_ERROR, "操作数据库失败");
            return removeResult;
        });
        return true;
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // DTO转实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest,picture);
        //参数 整理
        String tagsJsonStr = JSONUtil.toJsonStr(pictureEditRequest.getTags());
        picture.setTags(tagsJsonStr);
        //如果该id的图片不存在：
        Picture oldPicture = this.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture==null,ErrorEnum.NOT_FOUND_ERROR);
        //校验权限
        //已经使用注解校验权限
        //this.checkPictureAuth(picture,loginUser);
        //校验参数合法性
        this.validPicture(picture);
        //图片审核数据处理：
        this.pictureReviewPretreatment(picture,loginUser);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorEnum.OPERATION_ERROR,"操作数据库失败");
        return result;
    }

    /**
     * 构建Picture的查询条件
     *
     * @param pictureQueryRequest 查询请求实体
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Boolean nullSpace = pictureQueryRequest.getNullSpace();

        //从多字段里模糊搜索
        if (!StringUtils.isBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale), "picScale", picScale);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        //queryWrapper.eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        //如果nullSpace为true表示查公共图库，spaceId为null的数据
        queryWrapper.isNull(nullSpace,"spaceId");
        //如果查的是个人空间
        queryWrapper.eq(!nullSpace,"spaceId",spaceId);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片的封装（实体转VO）
     *
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
    public Page<PictureVO> PicturePageToVOPage(Page<Picture> picturePage) {
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
            } else
                throw new BusinessException(ErrorEnum.SYSTEM_ERROR, "picture转VO错误");
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            pictureVO.setUserVO(userVO);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片数据，更新时使用
     *
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
        ThrowUtils.throwIf(p == null, ErrorEnum.NOT_FOUND_ERROR, "图片不存在");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorEnum.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorEnum.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public Boolean doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        //参数校验
        //审核选项合法（0、1、2）
        Long id = pictureReviewRequest.getId();
        int reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureStatusEnum pictureStatusEnum = PictureStatusEnum.getByValue(reviewStatus);
        ThrowUtils.throwIf(id == null || reviewMessage == null || pictureStatusEnum == null, ErrorEnum.PARAMS_ERROR);
        //查询是否有对应图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorEnum.NOT_FOUND_ERROR);
        //业务逻辑：
        //不能反复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorEnum.OPERATION_ERROR, "不能反复审核");
        }
        //更新审核状态
        User loginUser = userService.getLoginUser(request);
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        picture.setReviewStatus(reviewStatus);
        picture.setReviewMessage(reviewMessage);

        //操作数据库返回
        return this.updateById(picture);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        ThrowUtils.throwIf(count > 30 || count < 1, ErrorEnum.PARAMS_ERROR, "仅可导入1~30 条");
        // 要抓取的地址,两个占位符分别是：搜索词，从第几条开始；
        int pageIndex = RandomUtil.randomInt(200);
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1&first=%s", searchText, pageIndex);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorEnum.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorEnum.OPERATION_ERROR, "获取元素失败");
        }
        //Elements imgElementList = div.select("img.mimg"); 这里的div中的src是缩略图，
        // 下载原图：
        Elements imgElementList = div.select("a.iusc");  // 修改选择器，获取包含完整数据的元素
        //设计一个随机值，让他在这些图片中随机一个元素开始，否则每次抓取的都是同一批图片//
        int uploadCount = 0;
        int elementSize = imgElementList.size();
        int elementIndex = RandomUtil.randomInt(elementSize - count);
        for (int i = elementIndex; i < elementSize; i++) {
            //String fileUrl = imgElementList.get(i).attr("src");
            //上面获取的是缩略图，现在改为获取图片的原图地址
            // 获取data-m属性中的JSON字符串
            String dataM = imgElementList.get(i).attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix))
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(PictureQueryRequest pictureQueryRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        //限制只能看已过审的图片
        pictureQueryRequest.setReviewStatus(PictureStatusEnum.PASS.getValue());

        //查询数据库之前先查缓存

        //一、构建缓存key
        String condition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(condition.getBytes());
        String cacheKey = String.format("picture:getPictureListVO:%s", hashKey);
        //1、查本地缓存：
        Page<PictureVO> cachePage;
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null) {
            cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return cachePage;
        }
        //2、查redis缓存
        ValueOperations<String, String> opsedForValue = stringRedisTemplate.opsForValue();
        String cachedValue = opsedForValue.get(cacheKey);
        if (cachedValue != null) {
            cachePage = JSONUtil.toBean(cachedValue, Page.class);
            //放到本地缓存中
            String jsonStr = JSONUtil.toJsonStr(cachePage);
            LOCAL_CACHE.put(cacheKey, jsonStr);
            //返回
            return cachePage;
        }
        //未命中缓存：查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, pageSize),
                this.getQueryWrapper(pictureQueryRequest));
        //查出来转成脱敏后的数据：
        Page<PictureVO> pictureVOPage = this.PicturePageToVOPage(picturePage);
        //数据库查完，放到缓存中
        //1、序列化对象：
        String pageResult = JSONUtil.toJsonStr(pictureVOPage);
        //2、设置缓存时间，单位是 s
        long cacheExpireTime = 300 + RandomUtil.randomInt(0, 301);
        //3、插入redis缓存
        opsedForValue.set(cacheKey, pageResult, cacheExpireTime, TimeUnit.SECONDS);
        return pictureVOPage;
    }

    /**
     * 清理图片COS中的对象：
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture,Long spaceId) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        String url = oldPicture.getUrl();
        //判断是公共空间的还是私有空间的
        String key;
        if(spaceId != null){
            //私有空间
            key = url.substring(url.indexOf("space"));
        }else {
            //公共空间
            key = url.substring(url.indexOf("public"));//截取域名后的地址
        }
        cosManager.deleteObject(key);
        // 清理缩略图
        String tUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(tUrl)) {
            String tKey = tUrl.substring(tUrl.indexOf("public"));
            cosManager.deleteObject(tKey);
        }
    }

    /**
     * 校验改动图片是所需权限
     * @param picture
     * @param loginUser
     */
    @Override
    public void checkPictureAuth(Picture picture,User loginUser) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null){
            //公共区域的图片：
            if (!picture.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser))
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR,"没有该图片的权限");
        }else{
            //私有空间的图片，仅空间拥有者可以操作
            if (!picture.getUserId().equals(loginUser.getId()))
                throw new BusinessException(ErrorEnum.NO_AUTH_ERROR,"没有该图片的权限");
        }
    }


}




