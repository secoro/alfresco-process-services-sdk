package com.activiti.extension.api.dossier;

import com.activiti.extension.api.dossier.model.InvolvedDossier;
import com.activiti.extension.bean.aspects.Authority;
import com.activiti.extension.bean.utils.literal.Cluster;
import com.activiti.model.common.ResultListDataRepresentation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/enterprise/custom/dossiers",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class CustomDossierController {

    private final DossierService dossierService;

    /**
     * Retrieves all dossiernumbers a user is involved with.
     *
     * @param involvedUser the username of the involved user
     * @param assignments how the user is involved (assignee, candidate, completed) multiple values are allowed.
     * @param finishedAfter date after which processes are finished
     * @param startedAfter date after which processes are started
     * @return List of InvolvedDossier's
     */
    @GetMapping()
      public List<InvolvedDossier> getInvolvedDossiers(
            @RequestParam(name = "username") String involvedUser,
            @RequestParam(name = "assignments", required = false) List<String> assignments,
            @RequestParam(name = "finishedAfter", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date finishedAfter,
            @RequestParam(name = "startedAfter", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startedAfter) {
        log.debug("Getting involved dossiers. GET REST API called.");
        return dossierService.getInvolvedDossiers(involvedUser, assignments, finishedAfter, startedAfter);
    }

    /**
     * This API returns dossiers with candidate tasks for a given query parameter
     *
     * @param query    the query (for example M2205 or M22 or DELAYREQUEST)
     * @param type     the query type (for example dossierId or processType, has to be an existing process instance variable)
     * @param clusters the cluster
     * @param first    the first item page
     * @param maxItems the max amount of items shown
     * @return ResultListDataRepresentation<TaskRepresentation>
     */
    @Authority(requiredAuthorities = {"PROCES_REGISSEURS"})
    @GetMapping("/candidate-query")
    public ResultListDataRepresentation<InvolvedDossier> getDossiersWithCandidateTasks(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "type") String type,
            @RequestParam(name = "clusters") List<Cluster> clusters,
            @RequestParam(name = "first", required = false, defaultValue = "0") int first,
            @RequestParam(name = "maxItems", required = false, defaultValue = "25") int maxItems
    ) {
        log.debug("Getting candidate tasks. GET REST API called.");
        return dossierService.getDossiersWithCandidateTasks(query, type, clusters, first, maxItems);
    }
}
