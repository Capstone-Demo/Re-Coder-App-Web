package com.capstone.demo.oauth;

import com.capstone.demo.domain.entity.User;
import com.capstone.demo.domain.value.Role;
import com.capstone.demo.dto.security.PrincipalDetails;
import com.capstone.demo.oauth.provider.GoogleUserInfo;
import com.capstone.demo.oauth.provider.KaKaoUserInfo;
import com.capstone.demo.oauth.provider.NaverUserInfo;
import com.capstone.demo.oauth.provider.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2Service extends DefaultOAuth2UserService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User=super.loadUser(userRequest);
        OAuth2UserInfo oAuth2UserInfo=null;

        if(userRequest.getClientRegistration().getRegistrationId().equals("google")){
            oAuth2UserInfo=new GoogleUserInfo(oAuth2User.getAttributes());
        }
        else if(userRequest.getClientRegistration().getRegistrationId().equals("naver")){
            oAuth2UserInfo=new NaverUserInfo((Map) oAuth2User.getAttributes().get("response"));
        }
        else if(userRequest.getClientRegistration().getRegistrationId().equals("kakao")){
            oAuth2UserInfo=new KaKaoUserInfo(oAuth2User.getAttributes(),(Map)oAuth2User.getAttributes().get("kakao_account"));
        }

        User buildUser = User.createUser()
                .email(oAuth2UserInfo.getEmail())
                .password(bCryptPasswordEncoder.encode("hong"))
                .name(oAuth2UserInfo.getName())
                .role(Role.ROLE_USER)
                .build();
        return new PrincipalDetails(buildUser,oAuth2User.getAttributes());
    }
}
