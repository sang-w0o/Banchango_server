package com.banchango.images.service;

import com.banchango.admin.exception.AdminInvalidAccessException;
import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.common.dto.BasicMessageResponseDto;
import com.banchango.common.exception.InternalServerErrorException;
import com.banchango.domain.users.UserRole;
import com.banchango.domain.users.Users;
import com.banchango.domain.users.UsersRepository;
import com.banchango.domain.warehouseimages.WarehouseImages;
import com.banchango.domain.warehouseimages.WarehouseImagesRepository;
import com.banchango.domain.warehouses.Warehouses;
import com.banchango.domain.warehouses.WarehousesRepository;
import com.banchango.images.dto.ImageInfoResponseDto;
import com.banchango.warehouses.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class S3UploaderService {

    private S3Client s3Client;
    private final WarehouseImagesRepository warehouseImagesRepository;
    private final WarehousesRepository warehousesRepository;
    private final UsersRepository usersRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.access_key_id}")
    private String accessKey;

    @Value("${aws.secret_access_key}")
    private String secretKey;

    @PostConstruct
    public void setS3Client() {
        s3Client = S3Client.builder()
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                .region(Region.AP_NORTHEAST_2).build();
    }

    private String uploadFile(MultipartFile file) {
        try {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .contentLength(file.getSize())
                    .key(file.getOriginalFilename())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            S3Utilities s3Utilities = s3Client.utilities();
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucket)
                    .key(file.getOriginalFilename())
                    .build();
            return s3Utilities.getUrl(getUrlRequest).toString();
        } catch(IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    private void deleteFile(final String fileName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch(Exception exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    private boolean isUserAuthenticatedToModifyWarehouseInfo(Integer userId, Integer warehouseId) {
        List<Warehouses> warehouses = warehousesRepository.findByUserId(userId);
        for(Warehouses warehouse : warehouses) {
            if(warehouse.getId().equals(warehouseId)) {
                if(warehouse.getUserId().equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void doubleCheckAdminAccess(Integer userId) {
        Users user = usersRepository.findById(userId).orElseThrow(AdminInvalidAccessException::new);
        if(!user.getRole().equals(UserRole.ADMIN)) throw new AdminInvalidAccessException();
    }

    private ImageInfoResponseDto saveImage(Integer warehouseId, MultipartFile file, Boolean isMain) {
        Warehouses warehouse = warehousesRepository.findById(warehouseId).orElseThrow(WarehouseIdNotFoundException::new);
        String url = uploadFile(file);
        WarehouseImages image = WarehouseImages.builder().url(url).isMain(isMain).warehouse(warehouse).build();
        WarehouseImages savedImage = warehouseImagesRepository.save(image);
        return new ImageInfoResponseDto(savedImage);
    }

    private void checkCountOfExtraImage(Integer warehouseId) {
        if(warehouseImagesRepository.findByWarehouseIdAndIsMain(warehouseId, false).size() >= 5) {
            throw new WarehouseExtraImageLimitException();
        }
    }

    private void checkCountOfMainImage(Integer warehouseId) {
        if(warehouseImagesRepository.findByWarehouseIdAndIsMain(warehouseId, true).size() > 1) {
            throw new WarehouseMainImageAlreadyRegisteredException();
        }
    }

    private void checkIfFileToRemoveExists(Integer warehouseId, String fileName) {
        if(!warehouseImagesRepository.findByWarehouseIdAndUrlContaining(warehouseId, fileName).isPresent()) {
            throw new WarehouseExtraImageNotFoundException(fileName + "은(는) 저장되어 있지 않은 사진입니다.");
        }
    }

    private String checkIfMainImageToDeleteExists(Integer warehouseId) {
        List<WarehouseImages> images = warehouseImagesRepository.findByWarehouseIdAndIsMain(warehouseId, true);
        if(images.size() >= 1) {
            WarehouseImages image = images.get(0);
            String[] splitTemp = image.getUrl().split("/");
            String fileName = splitTemp[splitTemp.length - 1];
            return fileName;
        } else throw new WarehouseMainImageNotFoundException();
    }

    @Transactional
    public ImageInfoResponseDto uploadExtraImageByAdmin(MultipartFile file, String token, Integer warehouseId) {
        doubleCheckAdminAccess(JwtTokenUtil.extractUserId(token));
        checkCountOfExtraImage(warehouseId);
        return saveImage(warehouseId, file, false);
    }

    @Transactional
    public ImageInfoResponseDto uploadExtraImage(MultipartFile file, String token, Integer warehouseId) {
        if(!isUserAuthenticatedToModifyWarehouseInfo(JwtTokenUtil.extractUserId(token), warehouseId)) {
            throw new WarehouseInvalidAccessException();
        }
        checkCountOfExtraImage(warehouseId);
        return saveImage(warehouseId, file, false);
    }

    @Transactional
    public ImageInfoResponseDto uploadMainImageByAdmin(MultipartFile file, String token, Integer warehouseId) {
        doubleCheckAdminAccess(JwtTokenUtil.extractUserId(token));
        checkCountOfMainImage(warehouseId);
        return saveImage(warehouseId, file, true);
    }

    @Transactional
    public ImageInfoResponseDto uploadMainImage(MultipartFile file, String token, Integer warehouseId) {
        if(!isUserAuthenticatedToModifyWarehouseInfo(JwtTokenUtil.extractUserId(token), warehouseId)) {
            throw new WarehouseInvalidAccessException();
        }
        checkCountOfMainImage(warehouseId);
        return saveImage(warehouseId, file, true);
    }

    @Transactional
    public BasicMessageResponseDto deleteExtraImageByAdmin(String fileName, String token, Integer warehouseId) {
        doubleCheckAdminAccess(JwtTokenUtil.extractUserId(token));
        checkIfFileToRemoveExists(warehouseId, fileName);
        warehouseImagesRepository.deleteByWarehouseIdAndUrlContaining(warehouseId, fileName);
        deleteFile(fileName);
        return new BasicMessageResponseDto("삭제에 성공했습니다.");
    }

    @Transactional
    public BasicMessageResponseDto deleteExtraImage(String fileName, String token, Integer warehouseId) {
        if(!isUserAuthenticatedToModifyWarehouseInfo(JwtTokenUtil.extractUserId(token), warehouseId)) {
            throw new WarehouseInvalidAccessException();
        }
        checkIfFileToRemoveExists(warehouseId, fileName);
        warehouseImagesRepository.deleteByWarehouseIdAndUrlContaining(warehouseId, fileName);
        deleteFile(fileName);
        return new BasicMessageResponseDto("삭제에 성공했습니다.");
    }

    @Transactional
    public BasicMessageResponseDto deleteMainImageByAdmin(String token, Integer warehouseId) {
        doubleCheckAdminAccess(JwtTokenUtil.extractUserId(token));
        String fileName = checkIfMainImageToDeleteExists(warehouseId);
        deleteFile(fileName);
        warehouseImagesRepository.deleteByWarehouseIdAndUrlContaining(warehouseId, fileName);
        return new BasicMessageResponseDto("삭제에 성공했습니다.");
    }

    @Transactional
    public BasicMessageResponseDto deleteMainImage(String token, Integer warehouseId) {
        if(!isUserAuthenticatedToModifyWarehouseInfo(JwtTokenUtil.extractUserId(token), warehouseId)) {
            throw new WarehouseInvalidAccessException();
        }
        String fileName = checkIfMainImageToDeleteExists(warehouseId);
        deleteFile(fileName);
        warehouseImagesRepository.deleteByWarehouseIdAndUrlContaining(warehouseId, fileName);
        return new BasicMessageResponseDto("삭제에 성공했습니다.");
    }
}
