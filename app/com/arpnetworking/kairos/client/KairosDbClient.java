package com.arpnetworking.kairos.client;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.ActorMaterializer;
import com.arpnetworking.commons.builder.OvalBuilder;
import com.arpnetworking.kairos.client.models.MetricsQuery;
import com.arpnetworking.kairos.client.models.MetricsQueryResponse;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.oval.constraint.NotNull;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Client for accessing KairosDB APIs.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public class KairosDbClient {
    /**
     * Executes a query for datapoints from  KairosDB.
     *
     * @param query the query
     * @return the response
     */
    public CompletionStage<MetricsQueryResponse> queryMetrics(final MetricsQuery query) {
        try {
            final HttpRequest request = HttpRequest.POST(createUri(METRICS_QUERY_PATH))
                    .withEntity(ContentTypes.APPLICATION_JSON, _mapper.writeValueAsString(query));
            return fireRequest(request, MetricsQueryResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> CompletionStage<T> fireRequest(final HttpRequest request, final Class<T> responseType) {
        return _http.singleRequest(request, _materializer)
                .thenCompose(httpResponse -> httpResponse.entity().toStrict(READ_TIMEOUT.toMillis(), _materializer))
                .thenApply(body -> {
                    try {
                        return _mapper.readValue(body.getData().utf8String(), responseType);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private String createUri(final URI relativePath) {
        return _uri.resolve(relativePath).toString();
    }

    private KairosDbClient(final Builder builder) {
        final ActorSystem actorSystem = builder._actorSystem;
        _mapper = builder._mapper;
        _uri = builder._uri;

        _http = Http.get(actorSystem);
        _materializer = ActorMaterializer.create(actorSystem);
    }

    private final ObjectMapper _mapper;
    private final Http _http;
    private final ActorMaterializer _materializer;
    private final URI _uri;

    private static final URI METRICS_QUERY_PATH = URI.create("/api/v1/datapoints/query");
    private static final FiniteDuration READ_TIMEOUT = FiniteDuration.apply(30, TimeUnit.SECONDS);

    public static final class Builder extends OvalBuilder<KairosDbClient> {
        /**
         * Public constructor.
         */
        public Builder() {
            super(KairosDbClient::new);
        }

        /**
         * Sets the actor system to perform operations on.
         *
         * @param value the actor system
         * @return this Builder
         */
        public Builder setActorSystem(final ActorSystem value) {
            _actorSystem = value;
            return this;
        }

        /**
         * Sets the object mapper to use for serialization.
         *
         * @param value the object mapper
         * @return this Builder
         */
        public Builder setMapper(final ObjectMapper value) {
            _mapper = value;
            return this;
        }

        /**
         * Sets the base URI.
         *
         * @param value the base URI
         * @return this Builder
         */
        public Builder setUri(final URI value) {
            _uri = value;
            return this;
        }

        @NotNull
        @JacksonInject
        private ActorSystem _actorSystem;
        @NotNull
        @JacksonInject
        private ObjectMapper _mapper;
        @NotNull
        private URI _uri;
    }
}
