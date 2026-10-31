package com.ai.shared.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@Slf4j
public class MainController {

    @GetMapping("/")
    public String main(Model model){
        model.addAttribute("left", "main");
        model.addAttribute("center", "fragments/main");
        return "layout/main";
    }
    @GetMapping("/go/{chapter}")
    public String page(Model model, @PathVariable(name = "chapter") String chapter ){
        model.addAttribute("left", chapter);
        model.addAttribute("center", "fragments/"+chapter);
        return "layout/main";
    }
    @GetMapping("/go/{chapter}/{content}")
    public String content(Model model, @PathVariable(name = "content") String content, @PathVariable(name = "chapter") String chapter ){
        model.addAttribute("left", chapter);
        model.addAttribute("page", "pages/"+chapter+"/"+content);
        return "layout/main";
    }

}