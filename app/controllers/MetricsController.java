package controllers;

import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.view.MetricsQuery;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
@Singleton
public class MetricsController extends Controller {

    @Inject
    public MetricsController(final ObjectMapper mapper) {
        _mapper = mapper;
    }

    public CompletionStage<Result> query() {
        final JsonNode body = request().body().asJson();
        if (body == null) {
            LOGGER.warn()
                    .setMessage("null body for query")
                    .log();
            return CompletableFuture.completedFuture(Results.badRequest());
        }

        try {
            _mapper.treeToValue(body, MetricsQuery.class);
        } catch (final IOException e) {
            LOGGER.warn()
                    .setMessage("invalid body for query")
                    .setThrowable(e)
                    .log();
            return CompletableFuture.completedFuture(Results.badRequest());
        }
        return CompletableFuture.completedFuture(Results.ok());
    }

    private final ObjectMapper _mapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsController.class);
}
