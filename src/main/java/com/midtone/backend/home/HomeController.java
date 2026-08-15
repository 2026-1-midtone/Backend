package com.midtone.backend.home;

import com.midtone.backend.home.application.HomeDashboardResponse;
import com.midtone.backend.home.application.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/dashboard")
    public HomeDashboardResponse getDashboard() {
        return homeService.getDashboard();
    }
}
