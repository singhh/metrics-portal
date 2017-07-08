package controllers;

import assets.javascripts.mql.grammar.CollectingErrorListener;
import com.arpnetworking.mql.grammar.MqlBaseListener;
import com.arpnetworking.mql.grammar.MqlLexer;
import com.arpnetworking.mql.grammar.MqlListener;
import com.arpnetworking.mql.grammar.MqlParser;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.view.MetricsQuery;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.atn.PredictionMode;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Controller to vend metrics.
 *
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

        final MetricsQuery query;
        try {
            query = _mapper.treeToValue(body, MetricsQuery.class);
        } catch (final IOException e) {
            LOGGER.warn()
                    .setMessage("invalid body for query")
                    .setThrowable(e)
                    .log();
            return CompletableFuture.completedFuture(Results.badRequest());
        }

        final MqlLexer lexer = new MqlLexer(new ANTLRInputStream(query.getQuery()));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final MqlParser parser = new MqlParser(tokens);
//        parser.removeErrorListeners();
        final MqlListener listener = new MqlBaseListener() {

        };
        final CollectingErrorListener errorListener = new CollectingErrorListener();
        parser.addErrorListener(new DiagnosticErrorListener());
        parser.addErrorListener(errorListener);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        MqlParser.StatementContext statement;
        try {
             statement = parser.statement(); // STAGE 1
        }
        catch (final Exception ex) {
            tokens.reset(); // rewind input stream
            parser.reset();
            parser.getInterpreter().setPredictionMode(PredictionMode.LL);
            statement = parser.statement();  // STAGE 2
        }

        if (parser.getNumberOfSyntaxErrors() != 0) {
            // Build the error object
            final ObjectNode response = Json.newObject();
            final ArrayNode errors = response.putArray("errors");
            errorListener.getErrors().forEach(errors::add);
            return CompletableFuture.completedFuture(Results.badRequest(response));
        }
        return CompletableFuture.completedFuture(Results.ok(statement.getText()));
    }

    private final ObjectMapper _mapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsController.class);
}
