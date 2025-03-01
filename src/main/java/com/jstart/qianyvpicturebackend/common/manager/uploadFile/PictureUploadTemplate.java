package com.jstart.qianyvpicturebackend.common.manager.uploadFile;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.jstart.qianyvpicturebackend.common.manager.CosManager;
import com.jstart.qianyvpicturebackend.config.CosClientConfig;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.jstart.qianyvpicturebackend.exception.ThrowUtils;
import com.jstart.qianyvpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    // ...

    /**
     * 上传文件
     *
     * @param inputSource      文件来源
     * @param uploadPathPrefix 文件上传时的路径前缀（目录）
     * @return 上传后获取到的原图片信息
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1、校验图片
        checkPictureObject(inputSource);


        //2、获取原始文件名
        String originalFilename = getOriginalFilename(inputSource);
        String uuid = RandomUtil.randomString(16);
        String pictureSuffix = FileUtil.getSuffix(originalFilename);
        if (pictureSuffix.length()>4) {
            pictureSuffix = "webp";
        }
        //3、构建新文件名字
        String newFileName = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                pictureSuffix);

        //4、构建文件上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, newFileName);
        File file = null;
        try {
            //5、构建临时文件
            file = File.createTempFile(uploadPath, null);
            //6、填充上传文件
            processFile(inputSource, file);
            //7、上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //8、解析返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return getUploadPictureResult(imageInfo, uploadPath, originalFilename, file);

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new BusinessException(ErrorEnum.OPERATION_ERROR, "上传文件失败");
        } finally {
            //9、删除文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error, filepath = {}", file.getAbsoluteFile());//打印绝对路径
            }
        }

    }

    /**
     * 处理上传图片后的返回结果
     *
     * @param imageInfo
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult getUploadPictureResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File file) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        /**
         * NumberUtil 是一个工具类，round 方法用于对传入的数字进行四舍五入，第二个参数 2 表示四舍五入保留两位小数。
         * round 方法的返回值通常是一个 BigDecimal 对象，.doubleValue() 会将 BigDecimal 转换成 double 类型。
         */
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        String picFormat = imageInfo.getFormat();
        //返回封装结果：
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(picFormat);
        return uploadPictureResult;
    }

    /**
     * 检查图片格式
     *
     * @param inputSource
     */
    protected abstract void checkPictureObject(Object inputSource);

    /**
     * 获取原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 填充本地临时文件
     *
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;


}
