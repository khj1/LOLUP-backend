package com.lolup.lolup_project.riot.summoner.application;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.lolup.lolup_project.duo.domain.SummonerRank;
import com.lolup.lolup_project.duo.domain.SummonerStat;
import com.lolup.lolup_project.duo.domain.SummonerTier;
import com.lolup.lolup_project.riot.match.exception.NoSuchSummonerException;
import com.lolup.lolup_project.riot.summoner.application.dto.SummonerAccountDto;
import com.lolup.lolup_project.riot.summoner.exception.RiotApiBadResponseException;

import reactor.core.publisher.Mono;

@Service
public class SummonerService {

	private static final String ACCOUNT_INFO_REQUEST_URI = "/lol/summoner/v4/summoners/by-name/{summonerName}?api_key={apiKey}";
	private static final String SUMMONER_STAT_REQUEST_URI = "/lol/league/v4/entries/by-summoner/{encryptedSummonerId}?api_key={apiKey}";
	private static final int INITIAL_WINS = 0;
	private static final int INITIAL_LOSSES = 0;

	private final WebClient webClient;
	private final String apiKey;

	public SummonerService(@Qualifier("summonerWebClient") final WebClient webClient,
						   @Value("${security.riot.api-key}") final String apiKey) {
		this.webClient = webClient;
		this.apiKey = apiKey;
	}

	public SummonerAccountDto requestAccountInfo(final String summonerName) {
		return webClient
				.get()
				.uri(ACCOUNT_INFO_REQUEST_URI, summonerName, apiKey)
				.retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new NoSuchSummonerException()))
				.onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new RiotApiBadResponseException()))
				.bodyToMono(SummonerAccountDto.class)
				.block();
	}

	public SummonerStat requestSummonerStat(final String encryptedSummonerId, final String summonerName) {
		return webClient
				.get()
				.uri(SUMMONER_STAT_REQUEST_URI, encryptedSummonerId, apiKey)
				.retrieve()
				.bodyToMono(SummonerStat.class)
				.blockOptional()
				.orElse(createUnrankedSummonerStat(summonerName));
	}

	private SummonerStat createUnrankedSummonerStat(final String summonerName) {
		return SummonerStat.builder()
				.summonerName(summonerName)
				.tier(SummonerTier.UNRANKED)
				.rank(SummonerRank.UNRANKED)
				.wins(INITIAL_WINS)
				.losses(INITIAL_LOSSES)
				.build();
	}
}
