package com.lolup.lolup_project.token;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lolup.lolup_project.member.Member;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	void deleteByMember(final Member member);

	Optional<RefreshToken> findByRefreshToken(final String refreshToken);
}
