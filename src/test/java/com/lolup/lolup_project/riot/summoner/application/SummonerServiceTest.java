package com.lolup.lolup_project.riot.summoner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lolup.lolup_project.duo.domain.SummonerRank;
import com.lolup.lolup_project.duo.domain.SummonerStat;
import com.lolup.lolup_project.duo.domain.SummonerTier;
import com.lolup.lolup_project.riot.match.exception.NoSuchSummonerException;
import com.lolup.lolup_project.riot.summoner.application.dto.SummonerAccountDto;
import com.lolup.lolup_project.riot.summoner.exception.RiotApiBadResponseException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class SummonerServiceTest {

	private static final String MOCK_SERVER_BASE_URL = "http://localhost:%s";
	private static final String TEST_API_KEY = "testApiKey";
	private static final String SUMMONER_NAME = "testSummonerName";
	private static final String ENCRYPTED_SUMMONER_ID = "testEncryptedSummonerId";
	private static final String WEB_CLIENT_BAD_REQUEST = "HTTP/1.1 404";
	private static final String WEB_CLIENT_BAD_RESPONSE = "HTTP/1.1 500";

	private static MockWebServer mockWebServer;
	private static SummonerService summonerService;
	private static ObjectMapper objectMapper;

	@BeforeAll
	static void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		String baseUrl = String.format(MOCK_SERVER_BASE_URL, mockWebServer.getPort());
		WebClient webClient = WebClient.create(baseUrl);
		summonerService = new SummonerService(webClient, TEST_API_KEY);

		objectMapper = new ObjectMapper();
	}

	@AfterAll
	static void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@DisplayName("라이엇 계정 정보를 불러온다.")
	@Test
	void getAccountInfo() throws JsonProcessingException {
		SummonerAccountDto 라이엇_계정_정보_응답 = createSummonerAccountDto();

		mockWebServer.enqueue(new MockResponse()
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.setBody(objectMapper.writeValueAsString(라이엇_계정_정보_응답)));

		SummonerAccountDto API_호출_결과 = summonerService.requestAccountInfo(SUMMONER_NAME);

		assertThat(API_호출_결과)
				.usingRecursiveComparison()
				.isEqualTo(라이엇_계정_정보_응답);
	}

	@DisplayName("계정 정보 호출 시 잘못된 소환사 이름을 입력하면 예외를 반환한다.")
	@Test
	void getAccountInfoWithWrongSummonerName() {
		mockWebServer.enqueue(new MockResponse()
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.setStatus(WEB_CLIENT_BAD_REQUEST));

		assertThatThrownBy(() -> summonerService.requestAccountInfo(SUMMONER_NAME))
				.isInstanceOf(NoSuchSummonerException.class);
	}

	@DisplayName("계정 정보 호출 시 라이엇 API 서버 내부에서 문제가 발생하면 예외를 반환한다.")
	@Test
	void getAccountInfoWithBadResponseFromRiotApi() {
		mockWebServer.enqueue(new MockResponse()
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.setStatus(WEB_CLIENT_BAD_RESPONSE));

		assertThatThrownBy(() -> summonerService.requestAccountInfo(SUMMONER_NAME))
				.isInstanceOf(RiotApiBadResponseException.class);
	}

	@DisplayName("소환사의 랭크 정보를 불러온다.")
	@Test
	void getSummonerStat() throws JsonProcessingException {
		SummonerStat 소환사_랭크_정보_응답 = createSummonerStat();

		mockWebServer.enqueue(new MockResponse()
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.setBody(objectMapper.writeValueAsString(소환사_랭크_정보_응답)));

		SummonerStat API_호출_결과 = summonerService.requestSummonerStat(ENCRYPTED_SUMMONER_ID, SUMMONER_NAME);

		assertThat(API_호출_결과)
				.usingRecursiveComparison()
				.isEqualTo(소환사_랭크_정보_응답);
	}

	@DisplayName("소환사가 아직 리그에 배치되지 않았다면 언랭크 상태의 초기 객체를 반환한다.")
	@Test
	void getUnrankedSummonerStat() {
		SummonerStat 언랭크_소환사_랭크_정보_응답 = createUnrankedSummonerStat();

		mockWebServer.enqueue(new MockResponse()
				.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

		SummonerStat API_호출_결과 = summonerService.requestSummonerStat(ENCRYPTED_SUMMONER_ID, SUMMONER_NAME);

		assertThat(API_호출_결과)
				.usingRecursiveComparison()
				.isEqualTo(언랭크_소환사_랭크_정보_응답);

	}

	private SummonerAccountDto createSummonerAccountDto() {
		return SummonerAccountDto.builder()
				.id("id")
				.name("testName")
				.puuid("puuid")
				.accountId("accountId")
				.revisionDate(1L)
				.summonerLevel(1L)
				.profileIconId(1)
				.build();
	}

	private SummonerStat createSummonerStat() {
		return SummonerStat.builder()
				.summonerName(SUMMONER_NAME)
				.tier(SummonerTier.CHALLENGER)
				.rank(SummonerRank.I)
				.wins(100)
				.losses(100)
				.build();
	}

	private SummonerStat createUnrankedSummonerStat() {
		return SummonerStat.builder()
				.summonerName(SUMMONER_NAME)
				.tier(SummonerTier.UNRANKED)
				.rank(SummonerRank.UNRANKED)
				.wins(0)
				.losses(0)
				.build();
	}
}
