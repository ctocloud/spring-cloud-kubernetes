/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * @author Spencer Gibb
 */
public class KubernetesServerList extends AbstractServerList<KubernetesServer> {

	private final KubernetesClient kubernetes;
	private final KubernetesDiscoveryProperties properties;

	private String serviceId;

	public KubernetesServerList(KubernetesClient kubernetes, KubernetesDiscoveryProperties properties) {
		this.kubernetes = kubernetes;
		this.properties = properties;
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
		this.serviceId = clientConfig.getClientName();
	}

	@Override
	public List<KubernetesServer> getInitialListOfServers() {
		return getServers();
	}

	@Override
	public List<KubernetesServer> getUpdatedListOfServers() {
		return getServers();
	}

	private List<KubernetesServer> getServers() {
		if (this.kubernetes == null) {
			return Collections.emptyList();
		}
		EndpointsList endpointsList = this.kubernetes.endpoints().withField("metadata.name", this.serviceId).list();
		if (endpointsList == null || endpointsList.getItems().isEmpty()) {
			return Collections.emptyList();
		}

		List<KubernetesServer> servers = new ArrayList<>();

		for (Endpoints endpoints : endpointsList.getItems()) {
			String name = endpoints.getMetadata().getName();
			String uid = endpoints.getMetadata().getUid();

			for (EndpointSubset subset : endpoints.getSubsets()) {
				for (int i = 0; i < subset.getAddresses().size(); i++ ) {
					EndpointAddress address = subset.getAddresses().get(i);
					EndpointPort port = subset.getPorts().get(i);
					servers.add(new KubernetesServer(address.getIp(), port.getPort(), name, uid));
				}
			}
		}
		return servers;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("KubernetesServerList{");
		sb.append("serviceId='").append(this.serviceId).append('\'');
		sb.append('}');
		return sb.toString();
	}
}

