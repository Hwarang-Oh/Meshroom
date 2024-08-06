package com.ssafy.meshroom.backend.domain.session.application;

import com.ssafy.meshroom.backend.domain.OVToken.application.OVTokenService;
import com.ssafy.meshroom.backend.domain.contents.application.ContentsOrderService;
import com.ssafy.meshroom.backend.domain.session.dao.SessionRepository;
import com.ssafy.meshroom.backend.domain.session.dto.*;
import com.ssafy.meshroom.backend.domain.user.application.UserDetailService;
import com.ssafy.meshroom.backend.domain.user.domain.User;
import com.ssafy.meshroom.backend.domain.user.domain.UserRole;
import com.ssafy.meshroom.backend.global.auth.jwt.TokenProvider;
import com.ssafy.meshroom.backend.global.common.dto.Response;
import com.ssafy.meshroom.backend.global.error.exception.FullCapacityLimitException;
import com.ssafy.meshroom.backend.global.error.exception.OpenViduException;
import com.ssafy.meshroom.backend.global.error.exception.SessionNotExistException;
import com.ssafy.meshroom.backend.global.util.CookieUtil;
import io.openvidu.java.client.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
@Slf4j
public class SessionService {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final SessionRepository sessionRepository;
    private final ContentsOrderService contentsOrderService;
    private final UserDetailService userDetailService;
    private final OpenViduService openViduService;
    private final TokenProvider tokenProvider;
    private final OVTokenService ovTokenService;
    private final User user;


    @Transactional
    public Response<SessionCreateResponse> createSession(List<String> contents) throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openViduService.createSession();

        com.ssafy.meshroom.backend.domain.session.domain.Session savedSession = sessionRepository.save(
                com.ssafy.meshroom.backend.domain.session.domain.Session.builder()
                        .sessionId(session.getSessionId())
                        .url("url")
                        .isMain(true)
                        .maxUserCount(60L)
                        .maxSubuserCount(10L)
                        .mainSession(null)
                        .build()
        );

        contentsOrderService.saveContentsOrder(savedSession.get_id(), contents);

        return new Response<SessionCreateResponse>(true, 2010L, "SUCCESS",
                SessionCreateResponse.builder()
                        .sessionId(session.getSessionId())
                        .url("tmp_url")
                        .build()
        );
    }

    @Transactional
    public Response<SubSessionCreateResponse> createSubSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        Optional.ofNullable(openViduService.getSession(sessionId)).orElseThrow(SessionNotExistException::new);
        Session session = openViduService.createSession();

        sessionRepository.save(
                com.ssafy.meshroom.backend.domain.session.domain.Session.builder()
                        .sessionId(session.getSessionId())
                        .url("url")
                        .isMain(false)
                        .groupName("그룹")
                        .maxUserCount(60L)
                        .maxSubuserCount(10L)
                        .mainSession(sessionId)
                        .build()
        );

        simpMessagingTemplate.convertAndSend("/subscribe/sessions/" + sessionId, getSessionInfo(sessionId).getResult());

        return new Response<SubSessionCreateResponse>(true, 2010L, "SUCCESS",
                new SubSessionCreateResponse(session.getSessionId())
        );
    }

    @Transactional
    public Response<ConnectionCreateResponse> createConnection(String userName, String sessionId, HttpServletResponse response) throws OpenViduJavaClientException, OpenViduHttpException {
        log.info("Create Connection :: {}", sessionId);
        // 1. 세션 인원 검사
        Session session = Optional.ofNullable(openViduService.getSession(sessionId)).orElseThrow(SessionNotExistException::new);
        com.ssafy.meshroom.backend.domain.session.domain.Session meshSession = sessionRepository.findBySessionId(sessionId).orElseThrow(SessionNotExistException::new);
        long curCount = ovTokenService.getUserCountInSession(meshSession.get_id());

        AtomicReference<Boolean> isMain = new AtomicReference<>();
        AtomicReference<UserRole> userRole = new AtomicReference<>(UserRole.PARTICIPANT);
        AtomicReference<com.ssafy.meshroom.backend.domain.session.domain.Session> sessionAtomicReference = new AtomicReference<>();
        sessionRepository.findBySessionId(sessionId).ifPresentOrElse(session1 -> {
            sessionAtomicReference.set(session1);
            isMain.set(session1.getIsMain());
            if (session1.getIsMain()) {
                if (curCount >= session1.getMaxUserCount()) {
                    throw new FullCapacityLimitException();
                }
                if (curCount == 0) {
                    userRole.set(UserRole.FACILITATOR);
                }
            } else {
                if (curCount >= session1.getMaxSubuserCount()) {
                    throw new FullCapacityLimitException();
                }
                if (curCount == 0) {
                    userRole.set(UserRole.TEAM_LEADER);
                }
            }
        }, () -> {
            throw new RuntimeException("없는 세션");
        });

        // 2. 유저 정보 저장
        String userId = "";
        if (SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) {
            userId = userDetailService.saveUser(userName, userRole.get());
        } else {
            userId = SecurityContextHolder.getContext().getAuthentication().getName();
        }


        if (isMain.get()) {
            // 3-1. 유저 jwtToken 발행
            String jwtToken = tokenProvider.generateToken(userId, Duration.ofDays(10L));
            CookieUtil.addCookie(response, "token", jwtToken);
        }

        // 3-2. session 접속 토큰 발행
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(OpenViduRole.PUBLISHER)
                .data(userName)
                .build();
        Connection connection = session.createConnection(connectionProperties);
        String token = connection.getToken(); // Send this string to the client side

        // 4. user 컬렉션과 token 컬렉션에 관계 추가
        if(!ovTokenService.checkIfTokenExists(userId, sessionAtomicReference.get().get_id())){
            ovTokenService.save(sessionAtomicReference.get().get_id(), userId, token);
        }

        if(isMain.get()){
            simpMessagingTemplate.convertAndSend("/subscribe/sessions/" + sessionId, getSessionInfo(sessionId).getResult());
        }else{
            String mainSessionId = sessionAtomicReference.get().getMainSession();
            simpMessagingTemplate.convertAndSend("/subscribe/sessions/" + mainSessionId, getSessionInfo(mainSessionId).getResult());
        }

        return new Response<ConnectionCreateResponse>(true, 2010L, "SUCCESS"
                , new ConnectionCreateResponse(token));
    }

    @Transactional
    public Response<SessionInfoResponse> getSessionInfo(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
//        Optional.ofNullable(openViduService.getSession(sessionId)).orElseThrow(SessionNotExistException::new);
        AtomicReference<SessionInfoResponse> sessionInfo = new AtomicReference<>();
        List<SubSessionInfoResponse> subs = new ArrayList<>();
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            sessionRepository.findAllByMainSession(sessionId).ifPresent(subSessions -> {
                subSessions.forEach((subSession) -> {
                    try {
                        subs.add(getSubsession(sessionId, subSession.getSessionId(), subSession.get_id()).orElseThrow());
                    } catch (OpenViduJavaClientException | OpenViduHttpException e) {
                        throw new OpenViduException();
                    }
                });
            });
            sessionInfo.set(SessionInfoResponse.builder()
                    .maxUserCount(session.getMaxUserCount())
                    .currentUserCount(ovTokenService.getUserCountInSession(session.get_id()) )
                    .url(session.getUrl())
                    .groups(subs)
                    .build()
            );
        });

        return new Response<SessionInfoResponse>(true, 2010L, "SUCCESS",
                sessionInfo.get());
    }

    public Response<SubSessionInfoResponse> getSubSessionInfo(String sessionId, String subSessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        com.ssafy.meshroom.backend.domain.session.domain.Session subSession = sessionRepository.findBySessionId(subSessionId).orElseThrow(SessionNotExistException::new);
        return new Response<SubSessionInfoResponse>(true, 2000L, "SUCCESS"
                , getSubsession(sessionId, subSessionId, subSession.get_id()).orElseThrow());
    }

    public Optional<SubSessionInfoResponse> getSubsession(String sessionId, String subSessionId, String subSessionSid) throws OpenViduJavaClientException, OpenViduHttpException {
        AtomicReference<SubSessionInfoResponse> ret = new AtomicReference<>();

        sessionRepository.findBySessionId(subSessionId).ifPresentOrElse(
                (session) -> {
                    // 1. subSession과 session이 부모자식관계인지
                    if (!session.getMainSession().equals(sessionId)) {
                        throw new RuntimeException(sessionId + "의 하위세션이 아닙니다.");
                    }

                    ret.set(SubSessionInfoResponse.builder()
                            .sessionId(subSessionId)
                            .maxUserCount(sessionRepository.findBySessionId(sessionId).orElseThrow().getMaxSubuserCount())
                            .groupName(session.getGroupName())
                            .username(
                                    ovTokenService.getUsersInSession(session.get_id())
                            )
                            .build()
                    );
                }
                , () -> {
                    throw new RuntimeException();
                }
        );
        ret.get().setCurrentUserCount(ovTokenService.getUserCountInSession(subSessionSid));
//        ret.get().setUsername(openViduService.getUsernameInSession(session))
        return Optional.ofNullable(ret.get());
    }

    @Transactional
    public Response<?> deleteSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        com.ssafy.meshroom.backend.domain.session.domain.Session _session
                = sessionRepository.findBySessionId(sessionId).orElseThrow(SessionNotExistException::new);

        if (_session.getIsMain()) {
            ovTokenService.removeSession(_session.get_id());
            sessionRepository.deleteAllByMainSession(sessionId).orElseThrow()
                    .forEach((session -> {
                        try {
                            openViduService.getSession(session.getSessionId()).close();
                        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
                            throw new OpenViduException();
                        }
                    }));
            sessionRepository.deleteById(_session.get_id());
        }

        Session session = Optional.ofNullable(openViduService.getSession(sessionId)).orElseThrow(SessionNotExistException::new);
        session.close();

        return new Response<>(true, 2000L, "삭제 성공", null);
    }

    @Transactional
    public Response<?> updateSessionUserCounts(String sessionId, UpdateSessionRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        com.ssafy.meshroom.backend.domain.session.domain.Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(SessionNotExistException::new);

        session.setMaxUserCount(request.getMaxUserCount());
        session.setMaxSubuserCount(request.getMaxSubuserCount());
        sessionRepository.save(session);

        simpMessagingTemplate.convertAndSend("/subscribe/sessions/" + sessionId, getSessionInfo(sessionId).getResult());
        return new Response<>(true, 2000L, "세션 정보가 성공적으로 수정되었습니다.", null);
    }

    @Transactional
    public Response<?> updateSubSessionGroupName(String subsessionId, UpdateGroupNameRequest request) throws OpenViduJavaClientException, OpenViduHttpException {
        com.ssafy.meshroom.backend.domain.session.domain.Session session = sessionRepository.findBySessionId(subsessionId)
                .orElseThrow(SessionNotExistException::new);

        session.setGroupName(request.getGroupName());
        sessionRepository.save(session);

        simpMessagingTemplate.convertAndSend("/subscribe/sessions/" + session.getMainSession(), getSessionInfo(session.getMainSession()).getResult());
        return new Response<>(true, 2000L, "하위 세션의 그룹 이름이 성공적으로 수정되었습니다.", null);
    }

    @Transactional
    public Response<?> removeUserFromSession(String sessionSid, String userSid) throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openViduService.getSession(sessionSid);

        // OpenVidu에서 사용자를 세션에서 제거
        Connection connection = session.getConnections().stream()
                .filter(conn -> conn.getConnectionId().equals(userSid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found in session"));
        session.forceDisconnect(connection);

        // 데이터베이스에서 사용자 제거
        ovTokenService.removeUserFromSession(sessionSid, userSid);

        return new Response<>(true, 2000L, "사용자가 세션에서 성공적으로 제거되었습니다.", null);
    }


}
