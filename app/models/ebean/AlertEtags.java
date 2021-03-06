/**
 * Copyright 2016 Smartsheet.com
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
package models.ebean;

import com.avaje.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Model class to represent alert etag records.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
// CHECKSTYLE.OFF: MemberNameCheck
@Entity
@Table(name = "alerts_etags", schema = "portal")
public class AlertEtags extends Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization")
    private Organization organization;

    @Column(nullable = false)
    private long etag;

    /**
     * Increments an etag record or creates one if it does not exist.
     *
     * @param organization the organization
     */
    public static void incrementEtag(final Organization organization) {
        AlertEtags etag = FINDER.setForUpdate(true)
                .where()
                .eq("organization", organization)
                .findUnique();
        if (etag == null) {
            etag = new AlertEtags();
            etag.setOrganization(organization);
            etag.setEtag(1);
        } else {
            etag.setEtag(etag.getEtag() + 1);
        }
        etag.save();
    }

    /**
     * Looks up an etag value for an organization.
     *
     * @param organization the organization
     * @return the etag value, or 0 if a value does not exist in the table
     */
    public static long getEtagByOrganization(final models.internal.Organization organization) {
        final AlertEtags etag = FINDER.where()
                .eq("organization.uuid", organization.getId())
                .findUnique();
        if (etag != null) {
            return etag.getEtag();
        }
        return 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long value) {
        id = value;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization value) {
        organization = value;
    }

    public long getEtag() {
        return etag;
    }

    public void setEtag(final long value) {
        etag = value;
    }

    private static final Finder<Long, AlertEtags> FINDER = new Finder<>(AlertEtags.class);
}
// CHECKSTYLE.ON: MemberNameCheck
