package com.fact_checker.FactChecker.controller;

import com.fact_checker.FactChecker.model.Video;
import com.fact_checker.FactChecker.service.UserService;
import com.fact_checker.FactChecker.service.VideoService;
import com.fact_checker.FactChecker.model.TermItem;
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
import java.util.List;

@Controller
@RequestMapping("/")
public class FactCheckerController implements ErrorController {

  private final VideoService videoService;
  private final UserService userService;

  public FactCheckerController(VideoService videoService, UserService userService) {
    this.videoService = videoService;
    this.userService = userService;
  }

  @GetMapping("/login")
  public String login() {
    return "login"; // This will render the login.html template
  }

  @GetMapping("/signup")
  public String signup() {
    return "signup"; // This will render the signup.html template
  }

  @PostMapping("/signup")
  // Can add other parameters here if needed
  public String signup(@RequestParam String username, @RequestParam String password, @RequestParam String email,
      @RequestParam String fullName, @RequestParam String confirmPassword, Model model) {

    if (username == null || password == null || email == null || fullName == null) {
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

  @GetMapping("/privacy-policy")
  public String privacyPolicy(HttpServletRequest request, Model model) {
    model.addAttribute("introduction",
        "At Truth Lens, we value your privacy and are committed to protecting your personal information. This Privacy Policy explains how we collect, use, and disclose information about you when you use our website and services.");

    model.addAttribute("personalInfo",
        "We may collect personal information such as your name, email address, and contact details when you sign up for our services, make a purchase, or contact us for support.");

    model.addAttribute("usageInfo",
        "We may collect information about how you use our website and services, such as the pages you visit, the features you use, and the time spent on our platform.");

    model.addAttribute("usageOfInfo",
        "We use the information we collect to provide, maintain, and improve our services, as well as to personalize your experience, communicate with you, and ensure the security of our platform.");

    model.addAttribute("sharingInfo",
        "We may share your information with third-party service providers who perform services on our behalf, such as hosting, data analysis, and customer service. We may also share information when required by law or to protect our rights.");

    return "privacy-policy";

  }

  @GetMapping("terms-of-service")
  public String termsOfService(HttpServletRequest request, Model model) {

    model.addAttribute("accountItems", List.of(
        new TermItem("Account Creation:",
            "To use our services, you must create an account by providing accurate and complete information."),
        new TermItem("Account Security:",
            "You are responsible for maintaining the confidentiality of your account credentials and for any activity that occurs under your account."),
        new TermItem("Account Termination:",
            "We reserve the right to suspend or terminate your account at any time for any reason, including if we reasonably believe you have violated these terms.")));

    model.addAttribute("conductItems", List.of(
        new TermItem("Prohibited Activities:",
            "You will not engage in any illegal, harmful, or fraudulent activities."),
        new TermItem("Intellectual Property:",
            "You will respect the intellectual property rights of others and not infringe on copyrights, trademarks, or other protected assets."),
        new TermItem("User Content:",
            "Any content you upload or share must be your own and not violate the rights of others.")));

    return "terms-of-service";

  }

  @GetMapping("/fact-check-video")
  public String uploadVideo() {
    return "upload-ui";
  }

  @PostMapping("/fact-check-video")
  public String factCheckVideo(@RequestParam("videoFile") MultipartFile videoFile,
      RedirectAttributes redirectAttributes) {
    if (videoFile.isEmpty() || videoFile.getSize() == 0 || videoFile.getOriginalFilename() == null) {
      redirectAttributes.addFlashAttribute("message", "Please upload a valid video file.");
      return "redirect:/fact-check-video";
    }

    if (videoFile.getSize() >= 1250000000) {

      redirectAttributes.addFlashAttribute("message", "File too big, please upload a smaller file.");
      return "redirect:/fact-check-video";
    }



    try {
      redirectAttributes.addFlashAttribute("message", "Processing video, please wait! ⌛");

      Video video = videoService.processAndSaveVideo(videoFile).join();
      String extractedText = video.getTranscriptionText();

      if (extractedText == null) {
        redirectAttributes.addFlashAttribute("message", "Could not extract text from video.");
        return "redirect:/fact-check-video";
      }
      redirectAttributes.addFlashAttribute("message", "Successfully uploaded file. " + extractedText);

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
