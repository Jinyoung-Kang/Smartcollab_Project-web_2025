package com.smartcollab.prod.repository;

import com.smartcollab.prod.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TeamMember 엔티티를 위한 Repository 인터페이스.
 */
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
}
