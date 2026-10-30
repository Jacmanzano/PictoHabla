package com.cmanzano.pictohabla.security;

import com.cmanzano.pictohabla.teacher.Teacher;
import com.cmanzano.pictohabla.teacher.TeacherRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final TeacherRepository teacherRepository;

    public CustomUserDetailsService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("El correo electrónico no puede estar vacío");
        }
        Teacher teacher = teacherRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró ningún profesor con el correo indicado"));
        return new SecurityUser(teacher);
    }
}
