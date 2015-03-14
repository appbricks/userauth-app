package org.appbricks.service.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Login controller simply returns reference to login view.
 */
@Controller
public class SignInController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SignInController.class);

    @RequestMapping(value = "/signin", method = RequestMethod.GET)
    public String showLoginPage() {
        
        return "main";
    }
}
