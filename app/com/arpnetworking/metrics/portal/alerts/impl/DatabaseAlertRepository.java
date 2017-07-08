/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.metrics.portal.alerts.impl;

import com.arpnetworking.metrics.portal.alerts.AlertRepository;
import com.arpnetworking.play.configuration.ConfigurationHelper;
import com.arpnetworking.steno.Logger;
import com.arpnetworking.steno.LoggerFactory;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.Junction;
import com.avaje.ebean.PagedList;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.google.inject.Inject;
import models.ebean.AlertEtags;
import models.ebean.NagiosExtension;
import models.internal.Alert;
import models.internal.AlertQuery;
import models.internal.Organization;
import models.internal.QueryResult;
import models.internal.impl.DefaultAlert;
import models.internal.impl.DefaultAlertQuery;
import models.internal.impl.DefaultQuantity;
import models.internal.impl.DefaultQueryResult;
import org.joda.time.Period;
import play.Configuration;
import play.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.PersistenceException;

/**
 * Implementation of <code>AlertRepository</code> using SQL database.
 *
 * @author Deepika Misra (deepika at groupon dot com)
 */
public class DatabaseAlertRepository implements AlertRepository {

    /**
     * Public constructor.
     *
     * @param environment Play's <code>Environment</code> instance.
     * @param config Play's <code>Configuration</code> instance.
     * @throws Exception If the configuration is invalid.
     */
    @Inject
    public DatabaseAlertRepository(final Environment environment, final Configuration config) throws Exception {
        this(
                ConfigurationHelper.<AlertQueryGenerator>getType(
                        environment,
                        config,
                        "alertRepository.alertQueryGenerator.type")
                        .newInstance());
    }

    /**
     * Public constructor.
     *
     * @param alertQueryGenerator Instance of <code>AlertQueryGenerator</code>.
     */
    public DatabaseAlertRepository(final AlertQueryGenerator alertQueryGenerator) {
        _alertQueryGenerator = alertQueryGenerator;
    }

    @Override
    public void open() {
        assertIsOpen(false);
        LOGGER.debug().setMessage("Opening alert repository").log();
        _isOpen.set(true);
    }

    @Override
    public void close() {
        assertIsOpen();
        LOGGER.debug().setMessage("Closing alert repository").log();
        _isOpen.set(false);
    }

    @Override
    public Optional<Alert> get(final UUID identifier, final Organization organization) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Getting alert")
                .addData("alertId", identifier)
                .addData("organization", organization)
                .log();

        final models.ebean.Alert ebeanAlert = Ebean.find(models.ebean.Alert.class)
                .where()
                .eq("uuid", identifier)
                .eq("organization.uuid", organization.getId())
                .findUnique();
        if (ebeanAlert == null) {
            return Optional.empty();
        }
        return Optional.of(convertFromEbeanAlert(ebeanAlert));
    }

    @Override
    public AlertQuery createQuery(final Organization organization) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Preparing query")
                .addData("organization", organization)
                .log();
        return new DefaultAlertQuery(this, organization);
    }

    @Override
    public QueryResult<Alert> query(final AlertQuery query) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Querying")
                .addData("query", query)
                .log();

        // Create the base query
        final PagedList<models.ebean.Alert> pagedAlerts = _alertQueryGenerator.createAlertQuery(query);

        // Compute the etag
        // TODO(deepika): Obfuscate the etag [ISSUE-7]
        final Long etag = _alertQueryGenerator.getEtag(query.getOrganization());

        final List<Alert> values = new ArrayList<>();
        pagedAlerts.getList().forEach(ebeanAlert -> values.add(convertFromEbeanAlert(ebeanAlert)));

        // Transform the results
        return new DefaultQueryResult<>(values, pagedAlerts.getTotalRowCount(), etag.toString());
    }

    @Override
    public long getAlertCount(final Organization organization) {
        assertIsOpen();
        return Ebean.find(models.ebean.Alert.class)
                .where()
                .eq("organization.uuid", organization.getId())
                .findRowCount();
    }

    @Override
    public int delete(final UUID identifier, final Organization organization) {
        LOGGER.debug()
                .setMessage("Deleting alert")
                .addData("id", identifier)
                .addData("organization", organization)
                .log();
        return Ebean.find(models.ebean.Alert.class)
                .where()
                .eq("uuid", identifier)
                .eq("organization.uuid", organization.getId())
                .delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOrUpdateAlert(final Alert alert, final Organization organization) {
        assertIsOpen();
        LOGGER.debug()
                .setMessage("Upserting alert")
                .addData("alert", alert)
                .addData("organization", organization)
                .log();


        try (Transaction transaction = Ebean.beginTransaction()) {
            models.ebean.Alert ebeanAlert = Ebean.find(models.ebean.Alert.class)
                    .where()
                    .eq("uuid", alert.getId())
                    .eq("organization.uuid", organization.getId())
                    .findUnique();
            boolean isNewAlert = false;
            if (ebeanAlert == null) {
                ebeanAlert = new models.ebean.Alert();
                isNewAlert = true;
            }

            ebeanAlert.setOrganization(models.ebean.Organization.findByOrganization(organization));
            ebeanAlert.setUuid(alert.getId());
            ebeanAlert.setNagiosExtension(convertToEbeanNagiosExtension(alert.getNagiosExtension()));
            ebeanAlert.setName(alert.getName());
            ebeanAlert.setQuery(alert.getQuery());
            ebeanAlert.setOperator(alert.getOperator());
            ebeanAlert.setPeriod(alert.getPeriod().toStandardSeconds().getSeconds());
            ebeanAlert.setQuantityValue(alert.getValue().getValue());
            ebeanAlert.setQuantityUnit(alert.getValue().getUnit().orElse(null));
            _alertQueryGenerator.saveAlert(ebeanAlert);
            transaction.commit();

            LOGGER.info()
                    .setMessage("Upserted alert")
                    .addData("alert", alert)
                    .addData("organization", organization)
                    .addData("isCreated", isNewAlert)
                    .log();
            // CHECKSTYLE.OFF: IllegalCatchCheck
        } catch (final IOException | RuntimeException e) {
            // CHECKSTYLE.ON: IllegalCatchCheck
            LOGGER.error()
                    .setMessage("Failed to upsert alert")
                    .addData("alert", alert)
                    .addData("organization", organization)
                    .setThrowable(e)
                    .log();
            throw new PersistenceException(e);
        }
    }

    private void assertIsOpen() {
        assertIsOpen(true);
    }

    private void assertIsOpen(final boolean expectedState) {
        if (_isOpen.get() != expectedState) {
            throw new IllegalStateException(String.format("Alert repository is not %s", expectedState ? "open" : "closed"));
        }
    }

    private Alert convertFromEbeanAlert(final models.ebean.Alert ebeanAlert) {
        return new DefaultAlert.Builder()
                .setId(ebeanAlert.getUuid())
                .setName(ebeanAlert.getName())
                .setQuery(ebeanAlert.getQuery())
                .setOperator(ebeanAlert.getOperator())
                .setPeriod(Period.seconds(ebeanAlert.getPeriod()).normalizedStandard())
                .setValue(new DefaultQuantity.Builder()
                        .setValue(ebeanAlert.getQuantityValue())
                        .setUnit(ebeanAlert.getQuantityUnit())
                        .build())
                .setNagiosExtension(convertToInternalNagiosExtension(ebeanAlert.getNagiosExtension()))
                .build();
    }


    private models.internal.NagiosExtension convertToInternalNagiosExtension(final NagiosExtension ebeanExtension) {
        if (ebeanExtension == null) {
            return null;
        }
        return new models.internal.NagiosExtension.Builder()
                .setSeverity(ebeanExtension.getSeverity())
                .setNotify(ebeanExtension.getNotify())
                .setMaxCheckAttempts(ebeanExtension.getMaxCheckAttempts())
                .setFreshnessThresholdInSeconds(ebeanExtension.getFreshnessThreshold())
                .build();
    }

    private NagiosExtension convertToEbeanNagiosExtension(final models.internal.NagiosExtension internalExtension) {
        if (internalExtension == null) {
            return null;
        }
        final NagiosExtension extension = new NagiosExtension();
        extension.setSeverity(internalExtension.getSeverity());
        extension.setNotify(internalExtension.getNotify());
        extension.setMaxCheckAttempts(internalExtension.getMaxCheckAttempts());
        extension.setFreshnessThreshold(internalExtension.getFreshnessThreshold().getStandardSeconds());
        return extension;
    }

    private final AtomicBoolean _isOpen = new AtomicBoolean(false);
    private final AlertQueryGenerator _alertQueryGenerator;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAlertRepository.class);

    /**
     * Inteface for database query generation.
     */
    public interface AlertQueryGenerator {

        /**
         * Translate the <code>AlertQuery</code> to an Ebean <code>Query</code>.
         *
         * @param query The repository agnostic <code>AlertQuery</code>.
         * @return The database specific <code>PagedList</code> query result.
         */
        PagedList<models.ebean.Alert> createAlertQuery(AlertQuery query);

        /**
         * Save the <code>Alert</code> to the database. This needs to be executed in a transaction.
         *
         * @param alert The <code>Alert</code> model instance to save.
         */
        void saveAlert(models.ebean.Alert alert);

        /**
         * Gets the etag for the alerts table.
         *
         * @param organization The organization owning the alert.
         * @return The etag for the table.
         */
        long getEtag(Organization organization);
    }

    /**
     * RDBMS agnostic query for alerts.
     */
    public static final class GenericQueryGenerator implements AlertQueryGenerator {

        @Override
        public PagedList<models.ebean.Alert> createAlertQuery(final AlertQuery query) {
            ExpressionList<models.ebean.Alert> ebeanExpressionList = Ebean.find(models.ebean.Alert.class).where();
            ebeanExpressionList = ebeanExpressionList.eq("organization.uuid", query.getOrganization().getId());

            //TODO(deepika): Add full text search [ISSUE-11]
            if (query.getContains().isPresent()) {
                final Junction<models.ebean.Alert> junction = ebeanExpressionList.disjunction();
                ebeanExpressionList = junction.contains("name", query.getContains().get());
                ebeanExpressionList = junction.contains("query", query.getContains().get());
                ebeanExpressionList = junction.contains("metric", query.getContains().get());
                ebeanExpressionList = junction.contains("statistic", query.getContains().get());
                ebeanExpressionList = junction.contains("operator", query.getContains().get());
                ebeanExpressionList = ebeanExpressionList.endJunction();
            }
            final Query<models.ebean.Alert> ebeanQuery = ebeanExpressionList.query();
            int pageOffset = 0;
            if (query.getOffset().isPresent()) {
                pageOffset = query.getOffset().get() / query.getLimit();
            }
            return ebeanQuery.findPagedList(pageOffset, query.getLimit());
        }

        @Override
        public void saveAlert(final models.ebean.Alert alert) {
            Ebean.save(alert);
        }

        @Override
        public long getEtag(final Organization organization) {
            return AlertEtags.getEtagByOrganization(organization);
        }
    }
}
