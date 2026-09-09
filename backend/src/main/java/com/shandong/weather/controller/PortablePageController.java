package com.shandong.weather.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Exact Vue routes only: API failures and missing assets must retain their status. */
@Controller
public class PortablePageController {
    @GetMapping({"/weather", "/analysis", "/comparison", "/management"})
    public String page() {
        return "forward:/index.html";
    }
}
