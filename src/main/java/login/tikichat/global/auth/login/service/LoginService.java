package login.tikichat.global.auth.login.service;

import login.tikichat.domain.user.model.Role;
import login.tikichat.domain.user.model.User;
import login.tikichat.domain.user.repository.UserRepository;
import login.tikichat.global.exception.auth.PasswordResetRequiredForSocialAccountException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class LoginService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일이 존재하지 않습니다."));

        if (user.getRole().equals(Role.SOCIAL)) {
            throw new PasswordResetRequiredForSocialAccountException();
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
