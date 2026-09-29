package ru.rostislav.cloudfilestorage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.rostislav.cloudfilestorage.entity.User;
import ru.rostislav.cloudfilestorage.repository.UserRepository;
import ru.rostislav.cloudfilestorage.security.UserDetailsImpl;

@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword()
        );
    }
}
