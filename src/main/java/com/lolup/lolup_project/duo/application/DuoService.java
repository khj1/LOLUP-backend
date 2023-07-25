package com.lolup.lolup_project.duo.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lolup.lolup_project.duo.application.dto.DuoDto;
import com.lolup.lolup_project.duo.application.dto.DuoResponse;
import com.lolup.lolup_project.duo.application.dto.DuoSaveRequest;
import com.lolup.lolup_project.duo.application.dto.SummonerDto;
import com.lolup.lolup_project.duo.domain.Duo;
import com.lolup.lolup_project.duo.domain.DuoRepository;
import com.lolup.lolup_project.duo.domain.SummonerPosition;
import com.lolup.lolup_project.duo.domain.SummonerStat;
import com.lolup.lolup_project.duo.domain.SummonerTier;
import com.lolup.lolup_project.duo.exception.DuoDeleteFailureException;
import com.lolup.lolup_project.duo.exception.NoSuchDuoException;
import com.lolup.lolup_project.member.domain.Member;
import com.lolup.lolup_project.member.domain.MemberRepository;
import com.lolup.lolup_project.member.exception.NoSuchMemberException;
import com.lolup.lolup_project.riot.match.application.MatchService;
import com.lolup.lolup_project.riot.match.application.dto.RecentMatchStatsDto;
import com.lolup.lolup_project.riot.riotstatic.RiotStaticService;
import com.lolup.lolup_project.riot.summoner.application.SummonerService;
import com.lolup.lolup_project.riot.summoner.application.dto.SummonerAccountDto;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DuoService {

	private final MatchService matchService;
	private final SummonerService summonerService;
	private final RiotStaticService riotStaticService;
	private final DuoRepository duoRepository;
	private final MemberRepository memberRepository;

	public DuoResponse findAll(final SummonerPosition position, final SummonerTier tier, final Pageable pageable) {
		Page<DuoDto> data = duoRepository.findAll(position, tier, pageable);

		return new DuoResponse(data, riotStaticService.getLatestGameVersion());
	}

	@Transactional
	public void save(final Long memberId, final DuoSaveRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(NoSuchMemberException::new);

		SummonerDto summonerDto = requestSummonerDto(request.getSummonerName());

		Duo duo = Duo.create(member, summonerDto, request.getPosition(), request.getDesc());
		duoRepository.save(duo);
	}

	private SummonerDto requestSummonerDto(final String summonerName) {
		SummonerAccountDto accountDto = summonerService.requestAccountInfo(summonerName);
		SummonerStat summonerStat = summonerService.requestSummonerStat(accountDto.getId(), accountDto.getName());
		RecentMatchStatsDto recentMatchStats = matchService.requestRecentMatchStats(summonerName,
				accountDto.getPuuid());

		return new SummonerDto(accountDto.getProfileIconId(), recentMatchStats.getLatestWinRate(), summonerStat,
				recentMatchStats.getChampionStats());
	}

	@Transactional
	public void update(final Long duoId, final SummonerPosition position, final String desc) {
		Duo duo = duoRepository.findById(duoId)
				.orElseThrow(NoSuchDuoException::new);

		duo.update(position, desc);
	}

	@Transactional
	public void delete(final Long duoId, final Long memberId) {
		duoRepository.findByIdAndMemberId(duoId, memberId)
				.orElseThrow(DuoDeleteFailureException::new);

		duoRepository.deleteById(duoId);
	}
}
