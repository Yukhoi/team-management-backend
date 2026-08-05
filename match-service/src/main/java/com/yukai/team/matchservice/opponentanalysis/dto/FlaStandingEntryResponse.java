package com.yukai.team.matchservice.opponentanalysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlaStandingEntryResponse {

    @JsonProperty("IdEquipe")
    private Long teamId;

    @JsonProperty("Nom")
    private String teamName;

    @JsonProperty("Points")
    private Integer points;

    @JsonProperty("IdChampionnat")
    private Long championnatId;

    @JsonProperty("Victoire")
    private Integer wins;

    @JsonProperty("Egalite")
    private Integer draws;

    @JsonProperty("Defaite")
    private Integer losses;

    @JsonProperty("Bp")
    private Integer goalsFor;

    @JsonProperty("Bc")
    private Integer goalsAgainst;

    @JsonProperty("Bonus")
    private Integer bonus;

    @JsonProperty("Forfait")
    private Integer forfeits;

    @JsonProperty("CinqDernier")
    private List<String> form;

    public List<String> getForm() {
        return form == null ? List.of() : form;
    }
}
