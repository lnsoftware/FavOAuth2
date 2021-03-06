package com.favccxx.favauth.web.oauth2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.favccxx.favauth.service.IFavAuthService;

@Controller
@RequestMapping("oauth2")
public class FavAccessTokenController {
	
	@Autowired
	IFavAuthService favAuthService;
	
	@RequestMapping("favaccesstoken")
	public HttpEntity accessToken(HttpServletRequest request) throws OAuthSystemException, OAuthProblemException{
		//构建OAuth请求
		OAuthTokenRequest tokenRequest = new OAuthTokenRequest(request);
		//获取OAuth客户端Id
		String clientId = tokenRequest.getClientId();
		//校验客户端Id是否正确
		if(!favAuthService.checkClientId(clientId)){
			OAuthResponse oAuthResponse = OAuthASResponse
					.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
					.setError(OAuthError.TokenResponse.INVALID_CLIENT)
					.setErrorDescription("无效的客户端Id")
					.buildJSONMessage();
			return new ResponseEntity(oAuthResponse.getBody(), HttpStatus.valueOf(oAuthResponse.getResponseStatus()));
		}
				
		
		//检查客户端安全KEY是否正确
		if(!favAuthService.checkClientSecret(tokenRequest.getClientSecret())){
			OAuthResponse response = OAuthResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
						.setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
						.setErrorDescription("客户端安全KEY认证失败！")
						.buildJSONMessage();
			return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
		}
		
		
		String authCode = tokenRequest.getParam(OAuth.OAUTH_CODE);
		//验证类型，有AUTHORIZATION_CODE/PASSWORD/REFRESH_TOKEN/CLIENT_CREDENTIALS
		if(tokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE).equals(GrantType.AUTHORIZATION_CODE.toString())){
			if(!favAuthService.checkAuthCode(authCode)){
				OAuthResponse response = OAuthResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
						.setError(OAuthError.TokenResponse.INVALID_GRANT)
		                .setErrorDescription("错误的授权码")  
		                .buildJSONMessage();
				return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
			}
		}
		
		//生成访问令牌
		OAuthIssuerImpl authIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
		String accessToken = authIssuerImpl.accessToken();
		favAuthService.addAccessToken(accessToken, favAuthService.getUsernameByAuthCode(authCode));
		
		//生成OAuth响应
		OAuthResponse response = OAuthASResponse
				.tokenResponse(HttpServletResponse.SC_OK)
				.setAccessToken(accessToken)
				.setExpiresIn(String.valueOf(favAuthService.getExpireIn()))
				.buildJSONMessage();
		return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
	}

}
