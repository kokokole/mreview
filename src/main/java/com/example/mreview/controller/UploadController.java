package com.example.mreview.controller;

import com.example.mreview.dto.UploadResultDTO;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnailator;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@Log4j2
public class UploadController {

    @Value("${com.example.upload.path}") //application.properties의 변수
    private String uploadPath;

    @PostMapping("/uploadAjax")
    public ResponseEntity<List<UploadResultDTO>> uploadFile(MultipartFile[] uploadFiles) {
        List<UploadResultDTO> resultDTOList = new ArrayList<>();
        for (MultipartFile uploadFile : uploadFiles) {

            //이미지 파일만 업로드하도록 기능 추가
            if (uploadFile.getContentType().startsWith("image") == false) {
                log.warn("this file is not image type");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            String originalName = uploadFile.getOriginalFilename(); //실제 파일이름을 가져옴(IE나 Edge는 업로드 파일의 경로도 같이 와서 이름만 가져오기 필요)
            String fileName = originalName.substring(originalName.lastIndexOf("\\") + 1);
            log.info("fileName : " + fileName);

            //날짜 폴더 생성
            String folderPath = makeFolder();

            //UUID
            String uuid = UUID.randomUUID().toString();

            //저장할 파일 이름 중간에 _ 를 이용하여 구분
            String saveName = uploadPath + File.separator + folderPath + File.separator + uuid + "_" + fileName;

            Path savePath = Paths.get(saveName);

            try {
                uploadFile.transferTo(savePath); //실제 이미지 저장

                //섬네일 생성
                String thumbnailSaveName = uploadPath + File.separator + folderPath + File.separator + "s_" + uuid + "_" + fileName;

                File thumbnailFile = new File(thumbnailSaveName);

                Thumbnailator.createThumbnail(savePath.toFile(), thumbnailFile, 100, 100);

                resultDTOList.add(new UploadResultDTO(fileName, uuid, folderPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return new ResponseEntity<>(resultDTOList, HttpStatus.OK);

        //업로드 기능을 구현할 때 고려해볼만한 3가지 사항
        /*
        1. 쉘 스크립트 파일을 통한 공격문제(==>파일컨텐츠 체크)
        2. 동일한 이름의 파일 문제(==>UUID등을 사용해서 원본파일명이 아닌 별도 고유값을 파일명으로 해서 저장)
        3. 동일한 폴더의 너무 많은 파일(폴더안에 파일 수가 제한되어 있음, ==>날짜별 폴더를 생성하여 해당 폴더에 저장)
         */
    }

    private String makeFolder() {
        String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = str.replace("//", File.separator);
        log.info(folderPath);
        //make folder
        File uploadPathFolder = new File(uploadPath, folderPath);
        log.info(uploadPathFolder.toString());
        log.info(uploadPathFolder.exists());
        if (uploadPathFolder.exists() == false) {
            log.info(uploadPathFolder.mkdirs());
        }
        return folderPath;
    }


    @GetMapping("/display")
    public ResponseEntity<byte[]> getFile(String fileName, String size) {
        ResponseEntity<byte[]> result = null;


        try {
            String srcFileName = URLDecoder.decode(fileName, "UTF-8");
            log.info("fileName : " + srcFileName);

            File file = new File(uploadPath + File.separator + srcFileName);

            if(size != null && size.equals("1")){
                file = new File(file.getParent(), file.getName().substring(2)); // size가 1인 경우 원본 파일 전송, s_가 붙은 썸네일 파일명에서 s_다음 글자부터 가져와 원본 파일명을 선택택
           }

            log.info("file : " + file);

            HttpHeaders header = new HttpHeaders();

            //MIME타입 처리
            header.add("Content-type", Files.probeContentType(file.toPath()));
            //파일 데이터 처리
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }


    @PostMapping("/removeFile")
    public ResponseEntity<Boolean> removeFile(String fileName) {
        String srcFileName = null;

        try {
            srcFileName = URLDecoder.decode(fileName, "UTF-8");
            File file = new File(uploadPath + File.separator + srcFileName);
            boolean result = file.delete(); //원본파일 삭제

            File thumbnail = new File(file.getParent(), "s_" + file.getName());

            result = thumbnail.delete(); //원본파일과 함께 섬네일파일도 삭제

            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
