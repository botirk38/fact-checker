package com.fact_checker.FactChecker.controller;


import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.service.UserService;
import com.fact_checker.FactChecker.service.VideoService;
import com.fact_checker.FactChecker.service.TextAnalysisService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;


@Controller
@RequestMapping("/")
public class FactCheckerController implements ErrorController {

    private final VideoService videoService;
    private final UserService userService;
    private final TextAnalysisService textAnalysisService;

    public FactCheckerController(VideoService videoService, UserService userService , TextAnalysisService textAnalysisService) {
        this.videoService = videoService;
        this.userService = userService;
        this.textAnalysisService = textAnalysisService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";  // This will render the login.html template
    }


    @GetMapping("/signup")
    public String signup() {
        return "signup";  // This will render the signup.html template
    }

    @PostMapping("/signup")
    // Can add other parameters here if needed
    public String signup(@RequestParam String username, @RequestParam String password, @RequestParam String email, @RequestParam String fullName, @RequestParam String confirmPassword, Model model) {

        if(username == null || password == null || email == null || fullName == null) {
            model.addAttribute("error", "Please fill out all fields");
            return "redirect:/signup";
        }


        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "redirect:/signup";
        }

        userService.registerUser(username, password, email, fullName);
        model.addAttribute("success", "User created successfully");


        return "redirect:/login";
    }




    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object error = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        model.addAttribute("error", error);
        model.addAttribute("status", status);

        return "error";
    }


    @GetMapping("/fact-check-video")
    public String uploadVideo() {
        return "upload-ui";
    }


    @PostMapping("/fact-check-video")
    public String factCheckVideo(@RequestParam("videoFile")MultipartFile videoFile, RedirectAttributes redirectAttributes) {
        if (videoFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please upload a valid video file.");
            return "redirect:/fact-check-video";
        }

        if(videoFile.getOriginalFilename() == null) {
            redirectAttributes.addFlashAttribute("message", "Please upload a valid video file with a valid name.");
            return "redirect:/fact-check-video";
        }

        try {
            Video video = videoService.processAndSaveVideo(videoFile).join();
            String extractedText = video.getTranscriptionText();

            if (extractedText == null) {
                redirectAttributes.addFlashAttribute("message", "Could not extract text from video.");
                return "redirect:/fact-check-video";
            }

            int analysisScore =  textAnalysisService.analyzeText(extractedText);
            video.setFactPercentage(analysisScore);
            redirectAttributes.addFlashAttribute("message", "Successfully uploaded file. " + "Analysis score : " + extractedText);
            if (analysisScore > 50) {
                redirectAttributes.addFlashAttribute("message", "Successfully uploaded file. " + "Analysis score : " + extractedText + " This video is likely to contain false information.");
            } else {
                redirectAttributes.addFlashAttribute("message", "Successfully uploaded file. " + "Analysis score : " + extractedText + " This video is likely to contain true information.");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not upload file." + e.getMessage());
            return "redirect:/fact-check-video";
        }



        return "redirect:/fact-check-video";
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("videos", videoService.getAllVideos());
        return "home";

    }


}
