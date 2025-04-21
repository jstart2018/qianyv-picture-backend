package com.jstart.qianyvpicturebackend.common.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.jstart.qianyvpicturebackend.config.CosClientConfig;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ResultEnum;
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

@Service
@Slf4j
@Deprecated //已经用模板方法代替，添加注解表示已经废弃
public class FileManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;  
  
    // ...

    /**
     *  上传文件
     * @param multipartFile 文件内容
     * @param uploadPathPrefix 文件上传时的路径前缀（目录）
     * @return 上传后获取到的原图片信息
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //校验图片
        checkPictureObject(multipartFile);

        /**
         * 构建图片上传地址
         */
        String originalFilename = multipartFile.getOriginalFilename();
        String uuid = RandomUtil.randomString(16);
        //构建新文件名字
        String newFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));

        //构建文件上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, newFileName);
        File file = null;
        try {
            //构建临时文件
            file = File.createTempFile(uploadPath, null);
            //转移文件内容：
            multipartFile.transferTo(file);
            //上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //解析返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            /**
             * NumberUtil 是一个工具类，round 方法用于对传入的数字进行四舍五入，第二个参数 2 表示四舍五入保留两位小数。
             * round 方法的返回值通常是一个 BigDecimal 对象，.doubleValue() 会将 BigDecimal 转换成 double 类型。
             */
            double picScale = NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();

            String picFormat = imageInfo.getFormat();
            //返回封装结果：
            UploadPictureResult uploadPictureResult = new UploadPictureResult();

            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(picFormat);
            return uploadPictureResult;

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new BusinessException(ResultEnum.OPERATION_ERROR,"上传文件失败");
        } finally {
            //删除文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error, filepath = {}", file.getAbsoluteFile());//打印绝对路径
            }
        }

    }

    private void checkPictureObject(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile==null, ResultEnum.PARAMS_ERROR,"文件不能为空");
        //校验文件大小
        Long MAX_OBJECT_SIZE = 5*1024*1024L;
        ThrowUtils.throwIf(multipartFile.getSize()>MAX_OBJECT_SIZE, ResultEnum.PARAMS_ERROR,"文件不能大于5MB");
        //校验文件后缀
        //获取原文件后缀：
        String originalSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义可上传的文件后缀：
        List<String> ALLOW_OBJECT_SUFFIX = Arrays.asList("png","jpg","jpeg","gif","webp");
        ThrowUtils.throwIf(!ALLOW_OBJECT_SUFFIX.contains(originalSuffix), ResultEnum.PARAMS_ERROR,"文件格式不符合要求");

    }


}
