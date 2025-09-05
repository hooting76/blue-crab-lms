package BlueCrab.com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("serverTime", LocalDateTime.now());
        model.addAttribute("status", "백엔드 서버 정상 작동중");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("service", "BlueCrab Backend Server");
        return "status";
    }
}
