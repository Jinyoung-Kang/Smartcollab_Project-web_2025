package com.smartcollab.prod.service;

import com.smartcollab.prod.entity.Invitation;
import com.smartcollab.prod.entity.Notification;
import com.smartcollab.prod.entity.User;
import com.smartcollab.prod.repository.NotificationRepository;
import com.smartcollab.prod.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;


     // 일반 알림을 생성 (초대와 관련 없는 경우).
    @Transactional
    public void createNotification(User user, String content, Notification.NotificationType type) {
        // 네 번째 인자인 invitation을 null로 하여 기존 메소드를 호출합니다.
        this.createNotification(user, content, type, null);
    }


     // 초대와 관련된 알림을 생성
    @Transactional
    public void createNotification(User user, String content, Notification.NotificationType type, Invitation invitation) {
        Notification notification = Notification.builder()
                .user(user)
                .content(content)
                .type(type)
                .invitation(invitation)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("알림을 수정할 권한이 없습니다.");
        }
        notification.markAsRead();
    }

    @Transactional
    public void deleteNotification(Long notificationId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("알림을 삭제할 권한이 없습니다.");
        }
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotifications(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Notification> notifications = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(notifications);
    }
}
