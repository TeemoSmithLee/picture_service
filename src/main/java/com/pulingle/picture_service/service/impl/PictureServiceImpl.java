package com.pulingle.picture_service.service.impl;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectMetadata;
import com.pulingle.picture_service.domain.dto.PictureDTO;
import com.pulingle.picture_service.domain.dto.RespondBody;
import com.pulingle.picture_service.domain.entity.Picture;
import com.pulingle.picture_service.mapper.PictureMapper;
import com.pulingle.picture_service.service.PictureService;
import com.pulingle.picture_service.utils.RespondBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * Created by 杨健 on 2018/3/30
 *
 * @Des: 图片上传服务实现类
 */
@Service("pictureService")
public class PictureServiceImpl implements PictureService {

    // endpoint以杭州为例，其它region请按实际情况填写
    @Value("${oss.endpoint}")
    private String ENDPOINT;
    // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建
    @Value("${oss.accessKeyId}")
    private String ACCESS_KEY_ID;
    @Value("${oss.accessKeySecret}")
    private String ACCESS_KEY_SECRET;
    //Bucket名
    @Value("${oss.bucketName}")
    private String BUCKET_NAME;
    @Value("${oss.maxFileSize}")
    private long MAX_FILE_SIZE;

    @Resource
    PictureMapper pictureMapper;


    @Override
    public RespondBody uploadPicture(PictureDTO pictureDTO) {
        RespondBody respondBody = new RespondBody();
        //文件大小检测
        if (pictureDTO.getFile().getSize() > MAX_FILE_SIZE) {
            respondBody = RespondBuilder.buildErrorResponse("文件大小大于:" + MAX_FILE_SIZE + "字节");
            return respondBody;
        }
        try {
            // 创建OSSClient实例
            OSSClient ossClient = new OSSClient(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
            String type = pictureDTO.getFile().getContentType();
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(type);
            // 上传
            String keyName = UUID.randomUUID().toString();
            InputStream fileContent = pictureDTO.getFile().getInputStream();
            ossClient.putObject(BUCKET_NAME, keyName, fileContent, meta);
            // 关闭client
            ossClient.shutdown();

            //构建图片信息
            Picture picture = new Picture();
            picture.setUserId(pictureDTO.getUserId());
            if (pictureDTO.getTitle() != null)
                picture.setTitle(pictureDTO.getTitle());
            if (pictureDTO.getContent() != null)
                picture.setContent(pictureDTO.getContent());
            if (pictureDTO.getAlbumId() != null)
                picture.setAlbumId(Long.valueOf(pictureDTO.getAlbumId()));
            Date date = new Date();
            picture.setUploadTime(date);
            picture.setPictureName(keyName);
            String pictureUrl = BUCKET_NAME + "." + ENDPOINT + "/" + keyName;
            picture.setPictureUrl(pictureUrl);
            picture.setSize(String.valueOf(pictureDTO.getFile().getSize()));
            //记录上传图片信息
            pictureMapper.insert(picture);
            respondBody = RespondBuilder.buildNormalResponse(picture);
        } catch (IOException ioE) {
            ioE.printStackTrace();
            respondBody = RespondBuilder.buildErrorResponse("IO错误,获取文件失败");
        }
        return respondBody;


    }


}