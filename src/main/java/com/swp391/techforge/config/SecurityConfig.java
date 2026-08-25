package com.swp391.techforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.swp391.techforge.service.authentication.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
			throws Exception {
		http
				.authenticationProvider(authenticationProvider)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/login",
								"/register",
								"/register/**",
								"/css/**",
								"/js/**",
								"/images/**",
								"/forgot-password",
								"/forgot-password/**",
								"/reset-password")
						.permitAll()

						.requestMatchers("/account/**", "/checkout/**", "/orders/**", "/customer/warranty/**")
						.authenticated()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/staff/**").hasAnyRole("STAFF_SALES", "STAFF_WARRANTY")
						.anyRequest().permitAll())

				.formLogin(form -> form
						.loginPage("/login")
						.successHandler(authenticationSuccessHandler())
						.failureHandler(authenticationFailureHandler())
						.permitAll())

				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.permitAll())

				.csrf(csrf -> csrf.disable());

		return http.build();
	}

	@Bean
	public org.springframework.security.web.authentication.AuthenticationSuccessHandler authenticationSuccessHandler() {
		return (request, response, authentication) -> {
			boolean isAdmin = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
			boolean isSaleStaff = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_STAFF_SALES"));
			boolean isWarrantyStaff = authentication.getAuthorities().stream()
					.anyMatch(a -> a.getAuthority().equals("ROLE_STAFF_WARRANTY"));
			if (isAdmin) {
				response.sendRedirect("/admin/products");
			} else if (isSaleStaff) {
				response.sendRedirect("/staff/orders");
			} else if (isWarrantyStaff) {
				response.sendRedirect("/staff/warranty");
			} else {
				response.sendRedirect("/");
			}
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationFailureHandler authenticationFailureHandler() {
		return (request, response, exception) -> {

			String error;

			if (exception instanceof DisabledException) {
				error = "locked";
			} else {
				error = "bad_credentials";
			}

			response.sendRedirect("/login?error=" + error);
		};
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(
			CustomUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);

		return provider;
	}
}