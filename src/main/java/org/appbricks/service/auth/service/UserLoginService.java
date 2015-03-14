package org.appbricks.service.auth.service;

import org.appbricks.repository.user.UserRepository;
import org.appbricks.model.user.LoggedInUser;
import org.appbricks.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.social.security.SocialUserDetailsService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Implements {@link UserLoginService}
 * service methods.
 */
@Service
public class UserLoginService
    implements UserDetailsService, SocialUserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginService.class);

    @Inject
    private UserRepository userRepository;

    /**
     * Override UserDetailsService.loadUserByUsername(String username) to
     *
     * return LoggedInUser instance.
     */
    public LoggedInUser loadUserByUsername(String username)
        throws UsernameNotFoundException {

        User user = userRepository.findByLoginName(username);
        if (user == null) {

            String msg = "User '" + username + "' does not exist in the database.";
            LOGGER.error(msg);

            throw new UsernameNotFoundException(msg);
        } else {
            LOGGER.debug("Found user {}.", user.toString());
            return user.getLoggedInUser();
        }
    }

    /**
     * Override SocialUserDetailsService.loadUserByUserId(String userId) to
     *
     * return LoggedInUser instance.
     */
    public LoggedInUser loadUserByUserId(String userId) 
        throws UsernameNotFoundException {

        return this.loadUserByUsername(userId);
    }
}
