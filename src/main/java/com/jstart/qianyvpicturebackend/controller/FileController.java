package com.jstart.qianyvpicturebackend.controller;

import com.jstart.qianyvpicturebackend.annotation.AuthCheck;
import com.jstart.qianyvpicturebackend.common.constant.UserConstant;
import com.jstart.qianyvpicturebackend.common.entity.Result;
import com.jstart.qianyvpicturebackend.common.manager.CosManager;
import com.jstart.qianyvpicturebackend.exception.BusinessException;
import com.jstart.qianyvpicturebackend.exception.ErrorEnum;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @Resource
    private CosManager cosManager;


    /**
     * 测试文件上传
     *
     * @param multipartFile 文件参数，通过form表达传入，需要@RequestPart注解指定
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public Result<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();//获取原文件名字，包含文件后缀
        String filepath = String.format("/test/%s", filename);//拼接文件路径
        File file = null;
        try {
            // 上传文件
            /**
             * public static File createTempFile(String prefix, String suffix)
             * 创建一个临时文件，参数分别是前缀、后缀
             * 同时这表明了文件路径，文件前缀+后缀= 文件名
             */
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);//转移文件
            cosManager.putObject(filepath, file);//这里filepath有层级关系时，文件存放到桶中也会有目录层级关系
            // 返回可访问地址
            return Result.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorEnum.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }


    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;//用于存储从存储服务中获取的文件内容流
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流,把文件的内容读到内存中作为字节数组，这样就可以直接写入到 HTTP 响应流中
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();


        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorEnum.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}
