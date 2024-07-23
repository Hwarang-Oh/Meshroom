package com.ssafy.meshroom.backend.domain.session.application;

import com.ssafy.meshroom.backend.domain.OVToken.application.OVTokenService;
import com.ssafy.meshroom.backend.domain.contents.application.ContentsOrderService;
import com.ssafy.meshroom.backend.domain.session.dao.SessionRepository;
import com.ssafy.meshroom.backend.domain.session.dto.ConnectionCreateResponse;
import com.ssafy.meshroom.backend.domain.session.dto.SessionCreateResponse;
import com.ssafy.meshroom.backend.domain.session.dto.SessionInfoResponse;
import com.ssafy.meshroom.backend.domain.session.dto.SubSessionCreateResponse;
import com.ssafy.meshroom.backend.domain.user.application.UserDetailService;
import com.ssafy.meshroom.backend.domain.user.domain.UserRole;
import com.ssafy.meshroom.backend.global.auth.jwt.TokenProvider;
import com.ssafy.meshroom.backend.global.common.dto.Response;
import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Service
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ContentsOrderService contentsOrderService;
    private final UserDetailService userDetailService;
    private final OpenViduService openViduService;
    private final TokenProvider tokenProvider;
    private final OVTokenService ovTokenService;



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

        contentsOrderService.saveContentsOrder(savedSession.get_id(),contents);

        return new Response<SessionCreateResponse>(true,2010L, "SUCCESS",
                SessionCreateResponse.builder()
                        .sessionId(session.getSessionId())
                        .url("tmp_url")
                        .build()
                );
    }


    public Response<SubSessionCreateResponse> createSubSession(String sessionId) throws OpenViduJavaClientException, OpenViduHttpException {
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

        return new Response<SubSessionCreateResponse>(true,2010L, "SUCCESS",
                new SubSessionCreateResponse(session.getSessionId())
        );
    }

    @Transactional
    public Response<ConnectionCreateResponse> createConnection(String userName, String sessionId, HttpServletResponse response) throws OpenViduJavaClientException, OpenViduHttpException{
        // 1. 세션 인원 검사
        Session session = openViduService.getSession(sessionId);
        long curCount = openViduService.getSessionCount(session);

        AtomicReference<UserRole> userRole = new AtomicReference<>(UserRole.PARTICIPANT);
        AtomicReference<com.ssafy.meshroom.backend.domain.session.domain.Session> sessionAtomicReference = new AtomicReference<>();
        sessionRepository.findBySessionId(sessionId).ifPresentOrElse(session1 -> {
            sessionAtomicReference.set(session1);
            if(session1.getIsMain()){
                if (curCount >= session1.getMaxUserCount()) {
                    throw new RuntimeException("꽉참");
                }
                if(curCount==0){
                    userRole.set(UserRole.FACILITATOR);
                }
            }else{
                if (curCount >= session1.getMaxSubuserCount()) {
                    throw new RuntimeException("꽉참");
                }
                if(curCount==0){
                    userRole.set(UserRole.TEAM_LEADER);
                }
            }
        }, () -> {
            throw new RuntimeException("없는 세션");
        });

        // 2. user 컬렉션과 token 컬렉션에 관계 추가
        String userId = userDetailService.saveUser(userName, userRole.get());
        ovTokenService.save(sessionAtomicReference.get().get_id(), userId);

        // 3-1. 유저 jwtToken 발행
        String jwtToken = tokenProvider.generateToken(userId, Duration.ofDays(10L));
        Cookie cookie = new Cookie("token", jwtToken);
        cookie.setHttpOnly(true); // HTTP-Only 속성 설정
//        cookie.setSecure(true); // HTTPS로만 전송되도록 설정 (필요에 따라)
        cookie.setPath("/"); // 쿠키의 유효 경로 설정
        cookie.setMaxAge(60 * 60 * 24); // 쿠키의 유효 기간 설정 (초 단위, 여기서는 1일)
        response.addCookie(cookie);

        // 3-2. session 접속 토큰 발행
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(OpenViduRole.PUBLISHER)
                .data(userName)
                .build();
        Connection connection = session.createConnection(connectionProperties);
        String token = connection.getToken(); // Send this string to the client side
        return new Response<ConnectionCreateResponse>(true, 2010L, "SUCCESS"
                ,new ConnectionCreateResponse(token));
    }

    public Response<SessionInfoResponse> getSubSessionInfo(String sessionId, String subSessionId) throws OpenViduJavaClientException, OpenViduHttpException {
        AtomicReference<SessionInfoResponse> ret = new AtomicReference<>();

        sessionRepository.findBySessionId(subSessionId).ifPresentOrElse(
                (session) -> {
                    // 1. subSession과 session이 부모자식관계인지
                    log.info(session.get_id());
                    if(!session.getMainSession().equals(sessionId)) { throw new RuntimeException(); }

                    ret.set(SessionInfoResponse.builder()
                            .sessionId(subSessionId)
                            .maxUserCount(session.getMaxSubuserCount())
                            .groupName(session.getGroupName())
                            .username(
                                    ovTokenService.getUsersInSession(session.get_id())
                            )
                            .build()
                    );
                    // 2. sessionId로 session entity 쿼리

                }
                ,()->{ throw new RuntimeException(); }
        );
        Session session = openViduService.getSession(sessionId);
        ret.get().setCurrentUserCount(openViduService.getSessionCount(session));
//        ret.get().setUsername(openViduService.getUsernameInSession(session))

        return new Response<SessionInfoResponse>(true, 2000L, "SUCCESS"
                ,ret.get());
    }
}
