package com.smartcollab.prod.repository;

import com.smartcollab.prod.entity.Invitation;
import com.smartcollab.prod.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    List<Invitation> findAllByInviterOrInvitee(User inviter, User invitee);
}