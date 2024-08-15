package com.fact_checker.FactChecker.controller;


import com.fact_checker.FactChecker.service.VideoProcessor;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class FactCheckerController implements ErrorController {

    private final VideoProcessor videoProcessor;

    public FactCheckerController(VideoProcessor videoProcessor) {
        this.videoProcessor = videoProcessor;
    }

    @GetMapping("/login")
    public String login() {
        return "login";  // This will render the login.html template
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // This will handle the login logic, should redirect to the upload video page


        return "redirect:/upload-video";
    }



    @GetMapping("/signup")
    public String signup() {
        return "signup";  // This will render the signup.html template
    }

    @PostMapping("/signup")
    // Can add other parameters here if needed
    public String signup(@RequestParam String username, @RequestParam String password){
       // This will handle the signup logic, should also login the user

        return "redirect:/upload-video";
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
        if (!Objects.equals(videoFile.getContentType(), "video/mp4")) {
            redirectAttributes.addFlashAttribute("message", "Please upload a valid video file.");
            return "redirect:/fact-check-video";
        }

        if(videoFile.getOriginalFilename() == null) {
            redirectAttributes.addFlashAttribute("message", "Please upload a valid video file with a valid name.");
            return "redirect:/fact-check-video";
        }

        try {
            videoProcessor.extractTextFromSpeech(videoFile.getInputStream(), videoFile.getOriginalFilename());



        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Could not upload file.");
            return "redirect:/fact-check-video";
        }


        redirectAttributes.addFlashAttribute("message", "Video fact checked successfully.");


        return "redirect:/fact-check-video";
    }
}
