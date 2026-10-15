package com.campushire.security;

import com.campushire.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CampusHireUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CampusHireUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .map(CampusHireUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User account was not found"));
    }
}
