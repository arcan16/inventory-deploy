package com.inventories.infr.security.filters;

import com.inventories.infr.security.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String headerToken = request.getHeader("Authorization");
        System.out.println("filtrando " + headerToken);

        if(headerToken!=null && headerToken.startsWith("Bearer ")){
            String stringToken = headerToken.substring(7);
            try {
                if(jwtUtils.isValidToken(stringToken)){
                    String username = jwtUtils.getUserFromToken(stringToken);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                }
            } catch (Exception e) {
                // El token es valido pero el usuario ya no existe con ese nombre
                // (renombrado o eliminado, ver AccountActivity/UserController#updateUser):
                // se deja la request sin autenticar en vez de que el filtro truene
                // con un 500, y el resto de la cadena la rechaza con 401 normalmente.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request,response);
    }
}
