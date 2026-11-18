package com.h2football.module.masterdata.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class MasterDataDtos {
    private MasterDataDtos() {}

    public record CountryRequest(@NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String isoCode,
                                 @NotBlank @Size(max = 120) String name) {}
    public record CountryResponse(Long id, String isoCode, String name) {}

    public record LeagueRequest(@NotBlank @Size(max = 80) String code,
                                @NotBlank @Size(max = 160) String name,
                                @NotNull @Positive Long countryId) {}
    public record LeagueResponse(Long id, String code, String name, Long countryId, String countryName) {}

    public record SeasonRequest(@NotNull @Positive Long leagueId,
                                @NotBlank @Size(max = 30) String seasonCode,
                                @NotBlank @Size(max = 80) String displayName,
                                @NotNull LocalDate startDate,
                                @NotNull LocalDate endDate) {}
    public record SeasonResponse(Long id, Long leagueId, String seasonCode, String displayName,
                                 LocalDate startDate, LocalDate endDate) {}

    public record TeamRequest(@NotBlank @Size(max = 160) String name,
                              @NotNull @Positive Long countryId) {}
    public record TeamResponse(Long id, String name, Long countryId, String countryName) {}
}