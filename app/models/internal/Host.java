/**
 * Copyright 2014 Groupon.com
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

package models.internal;

import java.util.Optional;

/**
 * Internal model interface for a host.
 *
 * @author Ville Koskela (ville dot koskela at inscopemetrics dot com)
 */
public interface Host {

    /**
     * Accessor for the hostname.
     *
     * @return The hostname.
     */
    String getHostname();

    /**
     * Accessor for the state of the metrics software stack on the host.
     *
     * @return The state of the metrics software stack on the host.
     */
    MetricsSoftwareState getMetricsSoftwareState();

    /**
     * Accessor for the cluster.
     *
     * @return The cluster.
     */
    Optional<String> getCluster();
}
