package org.example.hanviet;

import jakarta.annotation.PreDestroy;
import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Controller
public class AppController {

    private final OcrService ocrService;
    private final static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads";
    private final ImageRepository imageRepository;

    @Autowired
    public AppController(OcrService ocrService, ImageRepository imageRepository) {
        this.ocrService = ocrService;
        this.imageRepository = imageRepository;

        File uploadDirectory = new File(UPLOAD_DIRECTORY);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdir();
        }
//        File staticImageDirectory = new File("src/main/resources/static/image");
//        if (!staticImageDirectory.exists()) {
//            staticImageDirectory.mkdir();
//        }
    }

    @GetMapping("/")
    public String displayUploadForm() {
        return "index";
    }

    @PostMapping("/upload")
    public RedirectView uploadImage(Model model, @RequestParam("image") MultipartFile file)
            throws IOException, ParseException, URISyntaxException, InterruptedException
    {
        try {
//        store file
            ImageEntity imageEntity = new ImageEntity();
            imageRepository.save(imageEntity);
            imageEntity.setFilename(file.getOriginalFilename());

            String[] imageParts = file.getOriginalFilename().split("[.]");
            String imageExtension = imageParts[imageParts.length - 1];

            Path tempFilePath = Paths.get(
                    UPLOAD_DIRECTORY, "%d.temp.%s".formatted(imageEntity.getId(), imageExtension)
            );
            Files.write(tempFilePath, file.getBytes());

//        Path compressedFilePath = Paths.get(
//                "src/main/resources/static/image", "%d.jpg".formatted(imageEntity.getId())
//        );

            Path compressedFilePath = Paths.get(
                    UPLOAD_DIRECTORY, "%d.jpg".formatted(imageEntity.getId())
            );
            ocrService.compressImage(tempFilePath.toString(), compressedFilePath.toString());

//        do OCR
            String ocrJson = ocrService.doOcrImage(compressedFilePath.toString());
            String rawHanzi = ocrService.parseOcr(ocrJson);
            String filteredHanzi = ocrService.filterHanzi(rawHanzi);
            imageEntity.setHanzi(filteredHanzi);

//        get Han Viet
            List<List<String>> hanviets = ocrService.getHanviets(filteredHanzi);

            StringBuilder stringBuilder = new StringBuilder();
            for (List<String> stringList : hanviets) {
                if (stringList.size() == 1) {
                    stringBuilder.append(stringList.getFirst());
                } else {
                    stringBuilder.append("(");
                    stringBuilder.append(String.join("/", stringList));
                    stringBuilder.append(")");
                }
                stringBuilder.append(" ");
            }
            imageEntity.setHanviet(stringBuilder.toString());

//        get translation
            String translationJson = ocrService.translate(filteredHanzi);
            String translation = ocrService.parseTranslation(translationJson);
            imageEntity.setTranslation(translation);

            imageRepository.save(imageEntity);

            model.addAttribute("msg", "Uploaded images: " + file.getOriginalFilename());
            return new RedirectView("/view-image/%d".formatted(imageEntity.getId()));
        } catch (Exception e) {
            return new RedirectView("/app-error?q=%s".formatted(URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)));
        }
    }

    @GetMapping("/app-error")
    public String error(Model model, @RequestParam String q) {
        model.addAttribute("error", q);
        return "app_error";
    }

    @GetMapping("/view-image/{id}")
    public String getImageById(Model model, @PathVariable long id) {
        Optional<ImageEntity> optionalImageEntity = imageRepository.findById(id);
        if (optionalImageEntity.isEmpty()) {
            return "not_found";
        }
        ImageEntity imageEntity = optionalImageEntity.get();

        model.addAttribute("image_url", "/image/%d.jpg".formatted(id));
        model.addAttribute("hanzi", imageEntity.getHanzi());
        model.addAttribute("hanviet", imageEntity.getHanviet());
        model.addAttribute("translation", imageEntity.getTranslation());
        return "display_image";
    }

//    @GetMapping("/image/{filename}")
//    public ResponseEntity<BufferedImage> retrieveImage(@PathVariable String filename) {
//        try {
//            System.out.println(filename);
//            BufferedImage image = ImageIO.read(new File("uploads/" + filename));
//            return ResponseEntity.ok()
//                    .contentType(MediaType.IMAGE_JPEG)
//                    .body(image);
//        } catch (IOException e) {
//            return null;
//        }
//    }


    @PreDestroy
    public void destroy() {
//        FileSystemUtils.deleteRecursively(new File("src/main/resources/static/image"));
        FileSystemUtils.deleteRecursively(new File("uploads"));
    }

}
