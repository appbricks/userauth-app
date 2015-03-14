package org.appbricks.service.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.atomic.AtomicLong;

@Controller
public class MainController {

    @RequestMapping(value="/", method=RequestMethod.GET)
    public String greeting(@RequestParam(value="name", defaultValue="World") String name, Model model) {

        return "main";
    }
}