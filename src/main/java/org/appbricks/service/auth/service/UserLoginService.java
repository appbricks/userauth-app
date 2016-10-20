package org.appbricks.service.auth.service;

import org.appbricks.model.user.IndividualUser;
import org.appbricks.model.user.SocialConnection;
import org.appbricks.repository.user.SocialConnectionRepository;
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
import java.util.List;

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

    @Inject
    private SocialConnectionRepository socialConnectionRepository;

    /**
     * Override UserDetailsService.loadUserByUsername(String username) to
     *
     * return LoggedInUser instance.
     */
    public LoggedInUser loadUserByUsername(String username)
        throws UsernameNotFoundException {

        User user = userRepository.findByLoginName(username);
        if (!((IndividualUser) user).isRegistered()) {
            throw new UserNotRegisteredException("User %s is not registered.", user.getLoginName());
        }

        if (user == null) {

            String msg = "User '" + username + "' does not exist in the database.";
            LOGGER.error(msg);

            throw new UsernameNotFoundException(msg);
        } else {
            LOGGER.debug("Found user {}.", user.toString());

            LoggedInUser loggedInUser = user.getLoggedInUser();
            this.socialConnectionRepository.findByUserId(username)
                .forEach(connection -> loggedInUser.addSocialConnection(connection));

            return loggedInUser;
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
